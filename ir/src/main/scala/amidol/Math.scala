package amidol

import scala.util.parsing.combinator._
import scala.util.parsing.input._
import scala.util._

object Math {

  // TODO parametrize over number space
  // TODO debruijn index for variables 9so you know _what_ is closed over)
  sealed trait Expr

  case class Plus(lhs: Expr, rhs: Expr) extends Expr
  case class Mult(lhs: Expr, rhs: Expr) extends Expr
  case class Negate(e: Expr) extends Expr
  case class Inverse(e: Expr) extends Expr
  case class Variable(s: Symbol) extends Expr
  case class Literal(d: Double) extends Expr


  object Expr extends JavaTokenParsers with PackratParsers {

    // Simple arithmetic grammar with a packrat parser (cuz it's fast and I like my left recursion)
    lazy val parser: PackratParser[Expr] = {
      lazy val atom: PackratParser[Expr] =
        ( floatingPointNumber           ^^ { s => Literal(s.toDouble)  }
        | raw"[a-zA-Z]+".r              ^^ { v => Variable(Symbol(v))  }
        | "(" ~> term <~ ")"
        | "-" ~> atom                   ^^ { e => Negate(e) }
        )

      lazy val factor: PackratParser[Expr] =
        ( factor ~ "*" ~ atom          ^^ { case (l ~ _ ~ r) => Mult(l,         r ) }
        | factor ~ "/" ~ atom          ^^ { case (l ~ _ ~ r) => Mult(l, Inverse(r)) }
        | atom
        )

      lazy val term: PackratParser[Expr] =
        ( term ~ "+" ~ factor          ^^ { case (l ~ _ ~ r) => Plus(l,        r ) }
        | term ~ "-" ~ factor          ^^ { case (l ~ _ ~ r) => Plus(l, Negate(r)) }
        | factor
        )

      term
    }

    // Parse an arithmetic expression from a string
    def apply(input: String): Try[Expr] = parseAll(parser, input) match {
      case Success(matched, _) => scala.util.Success(matched)
      case Failure(msg, in) => scala.util.Failure(new Exception(s"Failed at ${in.pos}: $msg\n\n${in.pos.longString}"))
      case Error(msg, in) => scala.util.Failure(new Exception(s"Errored at ${in.pos}: $msg\n\n${in.pos.longString}"))
      case _ => scala.util.Failure(new Exception("Parser failed in an unexpected way"))
    }

    // Convenience implicits
    implicit def symbol2Variable(sym: Symbol): Variable = Variable(sym)
    implicit def double2Literal(double: Double): Literal = Literal(double)
  }

  implicit class ExprOps(expr: Expr) {

    // Turn into a  pretty string representation.
    //
    // Compatible with Python's syntax
    def prettyPrint(precedence: Int = 0): String = {
      val p = expr.precedence
      def wrap(s: String): String = if (precedence > expr.precedence) { "(" + s + ")" } else { s }
      expr match {
        case Plus(l,Negate(r))  => wrap(l.prettyPrint(p-1) + " - " + r.prettyPrint(p-1))
        case Plus(l,r)          => wrap(l.prettyPrint(p-1) + " + " + r.prettyPrint(p-1))
        case Mult(l,Inverse(r)) => wrap(l.prettyPrint(p-1) + " / " + r.prettyPrint(p-1))
        case Mult(l,r)          => wrap(l.prettyPrint(p-1) + " * " + r.prettyPrint(p-1))
        case Negate(x)          => wrap("-" + x.prettyPrint(p-1))
        case x: Inverse         => Mult(Literal(1), x).prettyPrint(precedence)
        case Variable(s)        => s.toString
        case Literal(d)         => d.toString
      }
    }

    def precedence: Int = expr match {
      case _: Plus => 6
      case _: Mult => 7
      case _: Negate => 8
      case _: Inverse => 9
      case _: Variable |
           _: Literal => Int.MaxValue
    }

    def eval(vals: Map[Symbol, Double] = Map()): Double = expr match {
      case Plus(lhs, rhs) => lhs.eval(vals) + rhs.eval(vals)
      case Mult(lhs, rhs) => lhs.eval(vals) * rhs.eval(vals)
      case Negate(e) => -e.eval(vals)
      case Inverse(e) => 1.0 / e.eval(vals)
      case Variable(s) => vals.getOrElse(s, throw new Exception(s"Unbound variable $s"))
      case Literal(d) => d
    }

  }

}
