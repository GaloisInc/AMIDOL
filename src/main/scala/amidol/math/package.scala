package amidol

import scala.util.parsing.combinator._
import scala.util.parsing.input._
import scala.util._

package object math {

  // TODO parametrize over number space
  // TODO debruijn index for variables 9so you know _what_ is closed over)
  sealed trait Expr

  case class Not(e: Expr) extends Expr
  case class And(lhs: Expr, rhs: Expr) extends Expr
  case class Or(lhs: Expr, rhs: Expr) extends Expr
  case class Comparision(lhs: Expr, op: ComparisionOp, rhs: Expr) extends Expr
  case class Plus(lhs: Expr, rhs: Expr) extends Expr
  case class Mult(lhs: Expr, rhs: Expr) extends Expr
  case class Negate(e: Expr) extends Expr
  case class Inverse(e: Expr) extends Expr
  case class Variable(s: Symbol) extends Expr
  case class Literal(d: Double) extends Expr

  sealed trait ComparisionOp
  case object GT extends ComparisionOp
  case object EQ extends ComparisionOp
  case object LT extends ComparisionOp

  object Expr extends JavaTokenParsers with PackratParsers {

    // Simple arithmetic grammar with a packrat parser (cuz it's fast and I like my left recursion)
    lazy val parser: PackratParser[Expr] = {
      lazy val atom: PackratParser[Expr] =
        ( floatingPointNumber           ^^ { s => Literal(s.toDouble)  }
        | raw"(?U)\p{L}[\p{L}_]*".r             ^^ { v => Variable(Symbol(v))  }
        | "(" ~> disj <~ ")"
        | "-" ~> atom                   ^^ { e => Negate(e) }
        | "!" ~> atom                   ^^ { e => Not(e) }
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

      lazy val comp: PackratParser[Expr] =
        ( term ~ ">"          ~ term   ^^ { case (l ~ _ ~ r) => Comparision(l, GT, r) }
        | term ~ ("==" | "=") ~ term   ^^ { case (l ~ _ ~ r) => Comparision(l, EQ, r) }
        | term ~ "<"          ~ term   ^^ { case (l ~ _ ~ r) => Comparision(l, LT, r) }
        | term
        )

      lazy val conj: PackratParser[Expr] =
        ( conj ~ ("&" | "&&") ~ comp   ^^ { case (l ~ _ ~ r) => And(l, r) }
        | comp
        )

      lazy val disj: PackratParser[Expr] =
        ( disj ~ ("|" | "||") ~ conj   ^^ { case (l ~ _ ~ r) => Or(l, r) }
        | conj
        )

      disj
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
        case Or(l,r)            => wrap(l.prettyPrint(p) + " || " + r.prettyPrint(p+1))
        case And(l,r)           => wrap(l.prettyPrint(p) + " && " + r.prettyPrint(p+1))
        case Comparision(l,GT,r)=> wrap(l.prettyPrint(p) + " > " + r.prettyPrint(p+1))
        case Comparision(l,EQ,r)=> wrap(l.prettyPrint(p) + " == " + r.prettyPrint(p+1))
        case Comparision(l,LT,r)=> wrap(l.prettyPrint(p) + " M " + r.prettyPrint(p+1))
        case Plus(l,Negate(r))  => wrap(l.prettyPrint(p) + " - " + r.prettyPrint(p+1))
        case Plus(l,r)          => wrap(l.prettyPrint(p) + " + " + r.prettyPrint(p+1))
        case Mult(l,Inverse(r)) => wrap(l.prettyPrint(p) + " / " + r.prettyPrint(p+1))
        case Mult(l,r)          => wrap(l.prettyPrint(p) + " * " + r.prettyPrint(p+1))
        case Negate(x)          => wrap("-" + x.prettyPrint(p))
        case Not(x)             => wrap("!" + x.prettyPrint(p))
        case x: Inverse         => Mult(Literal(1), x).prettyPrint(precedence)
        case Variable(s)        => s.name
        case Literal(d)         => d.toString
      }
    }

    def asVariable: Try[Variable] = expr match {
      case v: Variable => scala.util.Success(v)
      case e => scala.util.Failure(throw new Exception(s"Expression ${e.prettyPrint()} is not a variable"))
    }

    def asConstant: Try[Literal] = expr match {
      case l: Literal => scala.util.Success(l)
      case e => scala.util.Failure(throw new Exception(s"Expression ${e.prettyPrint()} is not a constant"))
    }

    def precedence: Int = expr match {
      case _: Or => 3
      case _: And => 4
      case _: Comparision => 5
      case _: Plus => 6
      case _: Mult => 7
      case _: Negate |
           _: Not => 8
      case _: Inverse => 9
      case _: Variable |
           _: Literal => Int.MaxValue
    }

    def eval(vals: Map[Symbol, Double] = Map()): Double = {
      implicit def boolToDouble(b: Boolean): Double = if (b) 1.0 else 0.0
      implicit def doubleToBool(d: Double): Boolean = d != 0.0

      expr match {
        case Not(e) => !e.eval(vals)
        case And(lhs, rhs) => lhs.eval(vals) && rhs.eval(vals)
        case Or(lhs, rhs) => lhs.eval(vals) || rhs.eval(vals)
        case Comparision(lhs, LT, rhs) => lhs.eval(vals) < rhs.eval(vals)
        case Comparision(lhs, EQ, rhs) => lhs.eval(vals) == rhs.eval(vals)
        case Comparision(lhs, GT, rhs) => lhs.eval(vals) > rhs.eval(vals)
        case Plus(lhs, rhs) => lhs.eval(vals) + rhs.eval(vals)
        case Mult(lhs, rhs) => lhs.eval(vals) * rhs.eval(vals)
        case Negate(e) => -e.eval(vals)
        case Inverse(e) => 1.0 / e.eval(vals)
        case Variable(s) => vals.getOrElse(s, throw new Exception(s"Unbound variable $s"))
        case Literal(d) => d
      }
    }

    def renameVariables(renamer: Renamer[Variable, Variable]): Expr = 
      mapVariables(renamer.getOrInsert(_))

    def applySubstitution(theta: Map[Variable, Expr]): Expr =
      mapVariables((v: Variable) => theta.getOrElse(v, v))

    def mapVariables(func: Variable => Expr): Expr = expr match {
      case Not(e) => Not(e.mapVariables(func))
      case And(lhs, rhs) => And(lhs.mapVariables(func), rhs.mapVariables(func))
      case Or(lhs, rhs) => Or(lhs.mapVariables(func), rhs.mapVariables(func))
      case Comparision(lhs, op, rhs) => Comparision(lhs.mapVariables(func), op, rhs.mapVariables(func))
      case Plus(lhs, rhs) => Plus(lhs.mapVariables(func), rhs.mapVariables(func))
      case Mult(lhs, rhs) => Mult(lhs.mapVariables(func), rhs.mapVariables(func))
      case Negate(e) => Negate(e.mapVariables(func))
      case Inverse(e) => Inverse(e.mapVariables(func))
      case v: Variable => func(v)
      case l: Literal => l
    }
  }

}
