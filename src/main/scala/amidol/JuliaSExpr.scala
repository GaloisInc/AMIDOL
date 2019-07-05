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
    | wholeNumber                       ^^ { n => SNum(n.toDouble) }  // number literal
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
    "  but instead got " + got,
  ).mkString
}

/** All of the code for recovering an AMIDOL model out of a Julia S-expression */
trait ExtractOps { expr: JuliaSExpr =>

  def extractModel(): Model = expr match {
    case
      SNested(List(SAtom("block"),
        SNested(List(
          SAtom("function"),
          SNested(List(SAtom("call"), SAtom("main"), SAtom("β"), SAtom("γ"), SAtom("μ"))),
          body @ SNested(SAtom("block") :: _)
        ))
      )) => (body).extractBody()

    case _ => throw new JuliaExtractException(
      "main function definition",
      expr
    )
  }

  def extractBody(): Model = expr match {
    case
      SNested(
        SAtom("block") ::
        SNested(List(SAtom("macrocall"), SSymbol("@grounding"), SNothing, grounding)) ::
        SNested(List(SAtom("macrocall"), SSymbol("@reaction"), SNothing, reaction)) ::
        SNested(List(SAtom("(=)"), SAtom("Δ"), delta)) ::
        SNested(List(SAtom("(=)"), SAtom("ϕ"), phi)) ::
        SNested(List(SAtom("(=)"), SAtom("Λ"), lambda)) ::
        SNested(List(SAtom("(=)"), SAtom(_), SNested(List(
          SAtom("call"),
          SNested(List(SAtom("."), SAtom("Petri"), SNested(List(SAtom("quote"), SQuote(SAtom("Model")))))),
          SAtom("g"),
          SAtom("Δ"),
          SAtom("ϕ"),
          SAtom("Λ")
        )))) ::
        _
      ) =>
        val (states, eventDescrs) = grounding.extractGrounding()
        val outputPredicates: Seq[OutputPredicate] = delta.extractDeltas()
        val inputPredicates: Seq[InputPredicate] = phi.extractPhis()
        val rates: Seq[(EventId,math.Expr[Double])] = lambda.extractLambdas()

        assert(rates.length == inputPredicates.length)
        assert(outputPredicates.length == inputPredicates.length)

        val events = (rates, inputPredicates, outputPredicates).zipped
          .map { case ((eventId, rate), inputPred, outputPred) =>
            eventId -> Event(
              eventId,
              rate,
              input_predicate = Some(inputPred),
              output_predicate = Some(outputPred),
              description = eventDescrs.get(eventId)
            )
          }
          .toMap

        Model(
          states,
          events,
        )

    case _ => throw new JuliaExtractException(
      "main function body (with grounding, reaction, delta, phi, and lambda)",
      expr
    )
  }

  def extractGrounding(): (Map[StateId, State], Map[EventId, String]) = expr match {
    case SNested(SAtom("block") :: groundings) =>
      val states = Map.newBuilder[StateId, State]
      val events = Map.newBuilder[EventId, String]
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
            Left(id -> State(id, math.Variable(Symbol(name)), Some(description)))
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
        val (variablesTup, sideEffectsTup) = entry.extractArrowTuple()
        val variables = variablesTup.extractTuple()
        val sideEffects = sideEffectsTup.extractTuple()
        assert(variables.length == sideEffects.length)

        val transitions = Map.newBuilder[StateId, math.Expr[Double]]
        for ((v, se) <- variables zip sideEffects) {
          val stateV = v.extractVariable()
          val stateId = StateId(stateV.s.name)
          val stateEffect = (se).extractMathExpr()

          stateEffect match {
            case math.Plus(v, other) if v == stateV => transitions += (stateId -> other)
            case other => transitions += (stateId -> math.Plus(other, math.Negate(stateV)))
          }
        }

        OutputPredicate(transitions.result())
      }

    case _ => throw new JuliaExtractException(
      "delta block, containing output predicates",
      expr
    )
  }

  /* Extract all the input predicates
   *
   * TODO: this ignores the LHS of the `->`
   */
  def extractPhis(): Seq[InputPredicate] = expr match {
    case SNested(SAtom("vect") :: entries) =>
      for (entry <- entries) yield {
        val (_, rawPredicate) = entry.extractArrowTuple()
        val predicate = rawPredicate.extractPredicate()
        InputPredicate(predicate)
      }

    case _ => throw new JuliaExtractException(
      "phi block, containing input predicates",
      expr
    )
  }

  /* Extract all of the rates
   *
   * TODO: this ignores the LHS of the `->` and assumes there is only an equation in the block.
   */
  def extractLambdas(): Seq[(EventId,math.Expr[Double])] = expr match {
    case SNested(SAtom("vect") :: entries) =>
      entries.map(entry => (entry).extractRate())

    case _ => throw new JuliaExtractException(
      "lambda block, containing rates",
      expr
    )
  }

  def extractRate(): (EventId, math.Expr[Double]) = expr match {
    case
      SNested(List(
        SAtom("(=)"),
        SNested(SAtom("call") :: SAtom(event) :: _),
        SNested(List(SAtom("block"), rate)),
      )) =>
        val id = EventId(event)
        val rateExpr = (rate).extractMathExpr()
        (id, rateExpr)

    case _ => throw new JuliaExtractException(
      "event rate of the form `λ₂(...) = begin <rate-expr> end`",
      expr
    )
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
        arg0.extractMathExpr(),
      )((acc, arg) =>
        math.Mult(
          acc,
          arg.extractMathExpr(),
        )
      )

    case SNested(SAtom("call") :: SAtom("+") :: arg0 :: args) =>
      args.foldLeft(
        arg0.extractMathExpr(),
      )((acc, arg) =>
        math.Plus(
          acc,
          arg.extractMathExpr(),
        )
      )

    case SNested(List(SAtom("call"), SAtom("/"), lhs, rhs)) =>
      math.Mult(
        lhs.extractMathExpr(),
        math.Inverse(rhs.extractMathExpr()),
      )

    case SNested(List(SAtom("call"), SAtom("-"), lhs, rhs)) =>
      math.Plus(
        lhs.extractMathExpr(),
        math.Negate(rhs.extractMathExpr()),
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
        rhs.extractMathExpr(),
      )

    case SNested(SAtom("&&") :: arg0 :: args) =>
      args.foldLeft(
        arg0.extractPredicate(),
      )((acc, arg) =>
        math.And(
          acc,
          arg.extractPredicate(),
        )
      )

    case SNested(SAtom("||") :: arg0 :: args) =>
      args.foldLeft(
        arg0.extractPredicate(),
      )((acc, arg) =>
        math.Or(
          acc,
          arg.extractPredicate(),
        )
      )

    case _ => throw new JuliaExtractException("boolean expression", expr)
  }
}

