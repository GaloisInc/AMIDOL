package amidol

import scala.sys.process.Process
import scala.util.{Try, Failure, Success}
import scala.util.Random
import scala.concurrent.duration._
import scala.collection.immutable.ArraySeq
import scala.collection.mutable

import java.io.File
import java.io.PrintWriter
import java.util.UUID

import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

case class LatexEquations(equations: List[String])
object LatexEquations extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val jsonFormat = jsonFormat1(LatexEquations.apply)
}

object LatexExtract {

  /** Extract a model from a system of differential equations
   *
   *  @param latexSourceEquations Latex source for each equation in the system
   *  @return an extracted model and palette items
   */
  def extractFromSource(latexSourceEquations: List[String]): Try[Model] = Try {

    val equations = mutable.Map.empty[math.Variable, math.Expr[Double]]
    val initials = mutable.Map.empty[math.Variable, math.Expr[Double]]
    val constants = mutable.Map.empty[math.Variable, Double]

    // Parse the equations
    for (equationSrc <- latexSourceEquations) {
      val (lhs, rhs) = LaTeX.equation(equationSrc).get

      def asDerivative(): Boolean = extractDerivative(lhs).toOption.map { variable =>
        if (equations.contains(variable)) {
          throw new IllegalArgumentException(
            s"The derivative of `${variable.prettyPrint()}` is defined twice"
          )
        } else if (constants.contains(variable)) {
          throw new IllegalArgumentException(
            s"`${variable.prettyPrint()}` can't be a variable (and have a derivative)\n" +
             "since it has been defined already as a constant"
          )
        }

        equations += variable -> rhs
      }.nonEmpty
      def asInitial(): Boolean = extractInitialCond(lhs).toOption.map { variable =>
        if (initials.contains(variable)) {
          throw new IllegalArgumentException(
            s"The initial condition of `${variable.prettyPrint()}` is defined twice"
          )
        } else if (constants.contains(variable)) {
          throw new IllegalArgumentException(
            s"`${variable.prettyPrint()}` can't be a variable (and have as initial\n" +
             "condition) since it has been defined already as a constant"
          )
        }

        initials += variable -> rhs
      }.nonEmpty
      def asConstant(): Boolean = extractConstant(lhs, rhs).toOption.map { case (constant, value) =>
        if (constants.contains(constant)) {
          throw new IllegalArgumentException(
            s"The constant `${constant.prettyPrint()}` is defined twice"
          )
        } else if (initials.contains(constant)) {
          throw new IllegalArgumentException(
            s"`${constant.prettyPrint()}` can't be a constant since it has initial conditions"
          )
        } else if (equations.contains(constant)) {
          throw new IllegalArgumentException(
            s"`${constant.prettyPrint()}` can't be a constant since it has a derivative"
          )
        }

        constants += (constant -> value)
      }.nonEmpty

      (asDerivative() || asInitial() || asConstant()) match {
        case true => ()
        case false => throw LatexExtractionException {
          s"Unable to parse equation `$equationSrc` as one of:\n" +
           "  - an initial condition,\n" +
           "  - a derivate equation,\n" +
           "  - a constant"
        }
      }
    }

    // Check all variables + constants are defined
    val variablesDefinedForEqns: Set[math.Variable] = equations.keySet.toSet | initials.keySet.toSet | constants.keySet.toSet
    for ((v,eqn) <- equations) {
      val undefinedVars = eqn.variables() &~ variablesDefinedForEqns
      if (undefinedVars.nonEmpty) {
        throw new IllegalArgumentException(
          s"Derivative equation for `${v.prettyPrint()}` references the following\n" +
          s"unbound variable(s): ${undefinedVars.map(_.prettyPrint()).mkString(" ")}"
        )
      }
    }

    val variablesDefinedForIntialConds: Set[math.Variable] = constants.keySet.toSet
    for ((v,eqn) <- initials) {
      val undefinedVars = eqn.variables() &~ variablesDefinedForIntialConds
      if (undefinedVars.nonEmpty) {
        throw new IllegalArgumentException(
          s"Initial condition equation for `${v.prettyPrint()}` references the following\n" +
          s"unbound variable(s): ${undefinedVars.map(_.prettyPrint()).mkString(" ")}"
        )
      }
    }

    // Extract a model
    val stateIds: Map[math.Variable, StateId] =
      (equations.keySet ++ initials.keySet)
        .view
        .map(v => v -> StateId(v.s.name))
        .toMap

    var eventIdCount = 0L
    def freshEventId(): EventId = {
      eventIdCount += 1L
      EventId(s"event_${eventIdCount}")
    }

    val states = Map.newBuilder[StateId, State]
    val events = Map.newBuilder[EventId, Event]

    for ((variable, stateId) <- stateIds) {
      states += stateId -> State(
        variable,
        description = None,
        initial_value = initials.getOrElse(variable, math.Literal(0.0))
      )

      // TODO: merge coefficients matching (up to negation) across states
      for (rate <- equations.get(variable)) {
        val eventId = freshEventId()
        events += eventId -> Event(
          rate,
          output_predicate = OutputPredicate(Map(stateId -> math.Literal(1.0)))
        )
      }
    }

    Model(states.result(), events.result(), constants.toMap)
  }

  /** Given an expression, try to interpret it as a `dX/dt` style derivative
   *
   *  @param derivativeEquation equation from which to extract the derivative
   *  @param derivativeVariable variable w.r.t. which derivation is happening
   *  @returns the variable being derived
   */
  private[amidol] def extractDerivative(
    derivativeEquation: math.Expr[Double],
    derivativeVariable: math.Variable = math.Variable(Symbol("t"))
  ): Try[math.Variable] = Try {
    val (num, den) = derivativeEquation match {
      case math.Mult(math.Mult(n, math.Inverse(d)), rhs: math.Variable) => (math.Mult(n, rhs), d)
      case math.Mult(n, math.Inverse(d)) => (n, d)
      case other => throw LatexExtractionException {
        val got = other.prettyPrint()
        s"Expected a derivative of the form `dX/dt`, but got `$got`"
      }
    }

    if (den != math.Variable(Symbol("d" + derivativeVariable.s.name)) &&
        den != math.Mult(math.Variable(Symbol("d")), derivativeVariable)) {
      throw LatexExtractionException {
        val want = "d" + derivativeVariable.prettyPrint()
        val got = den.prettyPrint()
        s"Expected derivative denominator to look like `$want`, but got `$got`"
      }
    }

    num match {
      case math.Mult(math.Variable(Symbol("d")), variable: math.Variable) =>
        variable
      case math.Variable(variable) if variable.name.head == 'd' =>
        math.Variable(Symbol(variable.name.tail))
      case other => throw LatexExtractionException {
        val got = other.prettyPrint()
        s"Expected derivative numerator to look like `dX`, but got `$got`"
      }
    }
  }

  /** Given an expression, try to interpret it as a `X_0` style initial condition
   *
   *  @param intialEquation equation from which to extract the condition
   *  @returns the variable being initialized
   */
  private[amidol] def extractInitialCond(
    intialEquation: math.Expr[Double]
  ): Try[math.Variable] = Try {
    intialEquation match {
      case v: math.Variable if v.s.name.endsWith("_0") =>
        math.Variable(Symbol(v.s.name.dropRight(2)))
      case math.Mult(n: math.Variable, math.Literal(0.0)) => n
      case other => throw LatexExtractionException {
        val got = other.prettyPrint()
        s"Expected an intiial condition of the form `X_0`, but got `$got`"
      }
    }
  }

  /** Given an expression, try to interpret it as a `c` style constant
   *
   *  @param constant equation from which to extract the constant
   *  @param equation constant equation
   *  @returns the constant being defined
   */
  private[amidol] def extractConstant(
    constant: math.Expr[Double],
    equation: math.Expr[Double]
  ): Try[(math.Variable, Double)] = Try {
    (constant,equation) match {
      case (n: math.Variable, c: math.Literal[Double @unchecked]) => (n, c.d)
      case (_: math.Variable, other) => throw LatexExtractionException {
        val got = other.prettyPrint()
        s"Expected a constant expression but got `$got`"
      }
      case (other, _) => throw LatexExtractionException {
        val got = other.prettyPrint()
        s"Expected a constant condition of the form `c`, but got `$got`"
      }
    }
  }
}

case class LatexExtractionException(message: String) extends Exception(message)
