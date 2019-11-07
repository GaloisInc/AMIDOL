package amidol

import scala.util.parsing.combinator._
import scala.util.parsing.input._
import scala.util._

/** Julia's `show_sexpr` outputs somewhat non-standard looking SExpr:
 *
 *   - lists are comma-delimited
 *   - atoms can have (presumably only one set of) parens
 *   - literals are a bit confusing (the symbol literal, 'nothing', quote)
 *
 * Consequently, we have to roll our own parser. *Sigh*
 */
sealed trait JuliaSExpr extends ExtractOps
case class SNested(args: List[JuliaSExpr]) extends JuliaSExpr
case class SAtom(atom: String) extends JuliaSExpr
case class SSymbol(sym: String) extends JuliaSExpr
case class SNum(number: Double) extends JuliaSExpr
case class SQuote(quoted: JuliaSExpr) extends JuliaSExpr
case object SNothing extends JuliaSExpr

object JuliaSExpr extends AmidolParser {

  lazy val sexpr: PackratParser[JuliaSExpr] =
    ( ":" ~> raw"[^\s,\(\)]+".r         ^^ { a => SAtom(a) }          // symbol
    | ":" ~> raw"\([^\s,\(\)]+\)".r     ^^ { a => SAtom(a) }          // parenthesized symbol
    | raw"#QuoteNode".r ~> sexpr        ^^ { a => SQuote(a) }         // quoted node
    | "(" ~> repsep(sexpr, ",") <~ ")"  ^^ { a => SNested(a) }        // nested expression
    | "Symbol(" ~> stringLiteral <~ ")" ^^ { s => mkSSymbol(s) }      // Symbol literal
    | "nothing"                         ^^ { _ => SNothing }          // "nothing" literal
    | floatingPointNumber               ^^ { n => SNum(n.toDouble) }  // number literal
    )

  private def mkSSymbol(rawStr: String): SSymbol =
    SSymbol(StringContext.treatEscapes(rawStr.tail.init))

  def apply(input: String): Try[JuliaSExpr] = runParser(sexpr, input)
}

// TODO: add position to errors with `located`
case class JuliaExtractException(
  expected: String,
  got: JuliaSExpr
) extends Exception {
  override def toString = List(
    "JuliaExtractException:",
    "  expected a " + expected,
    "  but instead got " + got
  ).mkString
}

/** All of the code for recovering an AMIDOL model out of a Julia S-expression */
trait ExtractOps { expr: JuliaSExpr =>

  def extractModel(): Model = expr match {
    case
      SNested(List(SAtom("block"),
        SNested(List(
          SAtom("function"),
          SNested(SAtom("call") :: SAtom("main") :: params),
          body @ SNested(SAtom("block") :: _)
        ))
      )) =>
      val parsedParams = params.map {
        case SAtom(p) => p
        case p => throw new JuliaExtractException("bad parameter", p)
      }
      (body).extractBody(parsedParams)

    case
      SNested(List(
        SAtom("function"),
        SNested(SAtom("call") :: SAtom("main") :: params),
        body @ SNested(SAtom("block") :: _)
      )) =>
      val parsedParams = params.map {
        case SAtom(p) => p
        case p => throw new JuliaExtractException("bad parameter", p)
      }
      (body).extractBody(parsedParams)

    case _ => throw new JuliaExtractException(
      "main function definition",
      expr
    )
  }


  def extractBody(parsedParams: List[String]): Model = expr match {
    case
      SNested(
        SAtom("block") ::
        SNested(List(SAtom("macrocall"), SSymbol("@grounding"), SNothing, grounding)) ::
        SNested(List(
          SAtom("macrocall"),
          SSymbol("@variables"),
          SNothing,
          SNested(SAtom("tuple") ::  variables)
        )) ::
        SNested(List(SAtom("(=)"), SAtom("Δ"), delta)) ::
        SNested(List(SAtom("(=)"), SAtom("ϕ"), phi)) ::
        SNested(List(SAtom("(=)"), SAtom("Λ"), lambda)) ::
        SNested(List(SAtom("(=)"), SAtom(_), SNested(List(
          SAtom("call"),
          SNested(List(SAtom("."), SAtom("Petri"), SNested(List(SAtom("quote"), SQuote(SAtom("Model")))))),
          _,
          SAtom("Δ"),
          SAtom("ϕ"),
          SAtom("Λ")
        )))) ::
        SNested(List(SAtom("(=)"), SAtom(_), SNested(
          SAtom("call") ::
          SNested(List(SAtom("."), SAtom("Petri"), SNested(List(SAtom("quote"), SQuote(SAtom("Problem")))))) ::
          _ ::
          SNested(SAtom("tuple") :: initial) ::
          _
        ))) ::
        _
      ) =>
        println("Extracting")
        val (states, eventDescrs) = grounding.extractGrounding()
        val outputPredicates: Seq[OutputPredicate] = delta.extractDeltas()
        val inputPredicates: Seq[InputPredicate] = phi.extractPhis()
        val rates: Seq[math.Expr[Double]] = lambda.extractLambdas()

        assert(rates.length == inputPredicates.length)
        assert(outputPredicates.length == inputPredicates.length)
        assert(eventDescrs.length == inputPredicates.length)

        assert(variables.length == initial.length)

        val initials = (variables zip initial).map { case (v, i) =>
          v.extractVariable() -> i.extractMathExpr()
        }.toMap

        val events = ((eventDescrs zip rates), inputPredicates, outputPredicates).zipped
          .map { case (((eventId, desc), rate), inputPred, outputPred) =>
            eventId -> Event(
              rate,
              input_predicate = Some(inputPred),
              output_predicate = outputPred,
              description = Some(desc)
            )
          }
          .toMap

        Model(
          states.toMap.mapValues(s => s.copy(initial_value = initials(s.state_variable))),
          events,
          constants = parsedParams.map { p =>
            val v = math.Variable(Symbol(p))
            v -> initials(v).asConstant.get.d
          }.toMap
        )

    case _ => throw new JuliaExtractException(
      "main function body (with grounding, variables, delta, phi, and lambda)",
      expr
    )
  }

  def extractGrounding(): (Seq[(StateId, State)], Seq[(EventId, String)]) = expr match {
    case SNested(SAtom("block") :: groundings) =>
      val states = List.newBuilder[(StateId, State)]
      val events = List.newBuilder[(EventId, String)]
      for (grounding <- groundings) {
        grounding.extractNounOrVerb() match {
          case Left(state) => states += state
          case Right(event) => events += event
        }
      }
      (states.result(), events.result())

    case _ => throw new JuliaExtractException(
      "grounding block containing nouns and verbs",
      expr
    )
  }

  def extractNounOrVerb(): Either[(StateId, State), (EventId, String)] = expr match {
    case
      SNested(List(
        SAtom("call"),
        SAtom("(=>)"),
        SAtom(name),
        SNested(SAtom("call") :: SAtom(nounOrVerb) :: SAtom(description) :: _)
      )) =>
        nounOrVerb match {
          case "Noun" =>
            val id = StateId(name)
            Left(id -> State(
              state_variable = math.Variable(Symbol(name)),
              description = Some(description),
              initial_value = 0.0
            ))
          case "Verb" =>
            val id = EventId(name)
            Right(id -> description)
        }

    case _ => throw new JuliaExtractException(
      "noun or verb grounding",
      expr
    )
  }

  /* Extract the `Δ` output predicates.
   *
   * Theses represent changes in states.
   */
  def extractDeltas(): Seq[OutputPredicate] = expr match {
    case SNested(SAtom("vect") :: entries) =>
      for (entry <- entries) yield {
        val transitions = entry.extractTuple()
          .map {
            case SNested(List(SAtom("call"), SAtom("~"), vari, newVar)) =>
              val stateV = vari.extractVariable()
              val stateId = StateId(stateV.s.name)
              val stateEffect = newVar.extractMathExpr()

              stateEffect match {
                case math.Plus(v, other) if v == stateV => (stateId -> other)
                case other => (stateId -> math.Plus(other, math.Negate(stateV)))
              }

            case other => throw new JuliaExtractException(
              "Delta side-effect is expected to take the form `<VAR>~<NEW-VAR>`",
              other
            )
          }
          .toMap

        OutputPredicate(transitions)
      }

    case _ => throw new JuliaExtractException(
      "delta block, containing output predicates",
      expr
    )
  }

  /* Extract all the input predicates */
  def extractPhis(): Seq[InputPredicate] = expr match {
    case SNested(SAtom("vect") :: entries) =>
      for (entry <- entries) yield {
        InputPredicate(entry.extractPredicate())
      }

    case _ => throw new JuliaExtractException(
      "phi block, containing input predicates",
      expr
    )
  }

  /* Extract all of the rates */
  def extractLambdas(): Seq[math.Expr[Double]] = expr match {
    case SNested(SAtom("vect") :: entries) => entries.map(_.extractMathExpr())

    case _ => throw new JuliaExtractException("rate expressions", expr)
  }

  def extractVariable(): math.Variable = expr match {
    case SAtom(id) => math.Variable(Symbol(id))
    case _ => throw new JuliaExtractException("state variable", expr)
  }

  /* Turn `(x,y,z) => Seq(x,y,z)` and `(x) => Seq(x)`.
   *
   * This warrants a function since the single case is a bit less usual: the parens aren't
   * represented in the AST at all
   */
  def extractTuple(): Seq[JuliaSExpr] = expr match {
    case SNested(SAtom("tuple") :: rest) => rest
    case single => Seq(single)
  }

  def extractArrowTuple(): (JuliaSExpr, JuliaSExpr) = expr match {
    case SNested(List(SAtom("->"), lhs, SNested(List(SAtom("block"), rhs)))) => lhs -> rhs
    case _ => throw new JuliaExtractException(
      "arrow tuple (something of the form `lhs -> rhs`)",
      expr
    )
  }

  /* Parse an arithmetic expression */
  def extractMathExpr(): math.Expr[Double] = expr match {
    case SNested(SAtom("call") :: SAtom("*") :: arg0 :: args) =>
      args.foldLeft(
        arg0.extractMathExpr()
      )((acc, arg) =>
        math.Mult(
          acc,
          arg.extractMathExpr()
        )
      )

    case SNested(SAtom("call") :: SAtom("+") :: arg0 :: args) =>
      args.foldLeft(
        arg0.extractMathExpr()
      )((acc, arg) =>
        math.Plus(
          acc,
          arg.extractMathExpr()
        )
      )

    case SNested(List(SAtom("call"), SAtom("/"), lhs, rhs)) =>
      math.Mult(
        lhs.extractMathExpr(),
        math.Inverse(rhs.extractMathExpr())
      )

    case SNested(List(SAtom("call"), SAtom("-"), lhs, rhs)) =>
      math.Plus(
        lhs.extractMathExpr(),
        math.Negate(rhs.extractMathExpr())
      )

    case SAtom(a) => math.Variable(Symbol(a))
    case SNum(n) => math.Literal(n)

    case _ => throw new JuliaExtractException("arithmetic expression", expr)
  }

  /* Parse a predicate expression */
  def extractPredicate(): math.Expr[Boolean] = expr match {
    case SNested(List(SAtom("call"), SAtom(">"), lhs, rhs)) =>
      math.Comparision(
        lhs.extractMathExpr(),
        math.GT,
        rhs.extractMathExpr()
      )

    case SNested(SAtom("&&") :: arg0 :: args) =>
      args.foldLeft(
        arg0.extractPredicate()
      )((acc, arg) =>
        math.And(
          acc,
          arg.extractPredicate()
        )
      )

    case SNested(SAtom("||") :: arg0 :: args) =>
      args.foldLeft(
        arg0.extractPredicate()
      )((acc, arg) =>
        math.Or(
          acc,
          arg.extractPredicate()
        )
      )

    case _ => throw new JuliaExtractException("boolean expression", expr)
  }
}

