package amidol

import amidol.math._

import scala.util.parsing.combinator._
import scala.util.parsing.input._
import scala.util._

object LaTeX extends AmidolParser {

  sealed trait Equation
  case class Derivative(variable: math.Variable, expression: math.Expr[Double]) extends Equation
  case class Initial(variable: math.Variable, expression: math.Expr[Double]) extends Equation
  case class Constant(variable: math.Variable, value: Double) extends Equation

  /** Parse out a simple latex equation from a string */
  val latexExpr: PackratParser[Expr[Double]] = {

    lazy val variable: PackratParser[Variable] =
      ( raw"(?U)\p{L}[\p{L}\p{No}_]*0?".r  ^^ { v => Variable(Symbol(v))  }
      | ( "\\alpha"
        | "\\beta"
        | "\\gamma"
        | "\\epsilon"
        | "\\mu"
        | "\\rho"
        | "\\sigma"
        )                                ^^ { v => Variable(Symbol(v))  }
      ).filter(_.s.name.last != '_')

    lazy val doubleAtom: PackratParser[Expr[Double]] =
      ( "-" ~> doubleAtom                ^^ { e => Negate(e) }
      | floatingPointNumber              ^^ { s => Literal(s.toDouble)  }
      | "[" ~> raw"[^]]+".r <~ "]"       ^^ { n => DataSeries(n) }
      | variable
      | "{" ~> term <~ "}"
      | "(" ~> term <~ ")"
      | "\\frac{" ~> term ~ "}{" ~ term <~ "}"
                                         ^^ { case (l ~ _ ~ r) => Mult(l, Inverse(r)) }
      )

    lazy val notNegateDouble: PackratParser[Expr[Double]] = doubleAtom.filter {
      case Negate(_) => false
      case _ => true
    }

    lazy val factor: PackratParser[Expr[Double]] =
      ( factor ~ ("*" | "\\times" | "\\cdot") ~ doubleAtom
                                         ^^ { case (l ~ _ ~ r) => Mult(l,         r ) }
      | factor ~ notNegateDouble         ^^ { case (l ~     r) => Mult(l,         r ) }
      | factor ~ "/" ~ doubleAtom        ^^ { case (l ~ _ ~ r) => Mult(l, Inverse(r)) }
      | doubleAtom
      )

    lazy val term: PackratParser[Expr[Double]] =
      ( term ~ "+" ~ factor              ^^ { case (l ~ _ ~ r) => Plus(l,        r ) }
      | term ~ "-" ~ factor              ^^ { case (l ~ _ ~ r) => Plus(l, Negate(r)) }
      | factor
      )

    term
  }


  lazy val equationExpr: PackratParser[(Expr[Double], Expr[Double])] =
    latexExpr ~ "=" ~ latexExpr          ^^ { case (l ~ _ ~ r) => (l, r) }

  def apply(input: String): Try[Expr[Double]] = runParser(latexExpr, input)
  def equation(input: String): Try[(Expr[Double], Expr[Double])] = runParser(equationExpr, input)
}


