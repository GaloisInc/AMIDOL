package amidol

import scala.sys.process.Process
import scala.util.{Try, Failure, Success}
import scala.util.Random
import scala.concurrent.duration._
import scala.collection.immutable.ArraySeq

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

    val equationsB = Map.newBuilder[math.Variable, math.Expr[Double]]
    val initialsB = Map.newBuilder[math.Variable, math.Expr[Double]]
    val constantsB = Map.newBuilder[math.Variable, Double]

    // Parse the equations
    for (equationSrc <- latexSourceEquations) {
      val (lhs, rhs) = LaTeX.equation(equationSrc).get

      def asDerivative(): Try[Any] = extractDerivative(lhs).map { equationsB += _ -> rhs }
      def asInitial(): Try[Any] = extractInitialCond(lhs).map { initialsB += _ -> rhs }
      def asConstant(): Try[Any] = extractConstant(lhs, rhs).map { constantsB += _ }

      (asDerivative() orElse asInitial() orElse asConstant()) match {
        case Success(_) => ()
        case Failure(_) => throw LatexExtractionException {
          s"Unable to parse equation `$equationSrc` as one of:\n" +
           "  - an initial condition,\n" +
           "  - a derivate equation,\n" +
           "  - a constant"
        }
      }
    }

    // Extract a model
    val equations = equationsB.result()
    val initials = initialsB.result()
    val constants = constantsB.result()
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

    Model(states.result(), events.result(), constants)
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

  /** Given an expression, try to interpret it as a `X(0)` style initial condition
   *
   *  @param intialEquation equation from which to extract the condition
   *  @returns the variable being initialized
   */
  private[amidol] def extractInitialCond(
    intialEquation: math.Expr[Double]
  ): Try[math.Variable] = Try {
    intialEquation match {
      case math.Mult(n: math.Variable, math.Literal(0.0)) => n
      case other => throw LatexExtractionException {
        val got = other.prettyPrint()
        s"Expected an intiial condition of the form `X(0)`, but got `$got`"
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
