package amidol

import amidol.math._

import scala.util.parsing.combinator._
import scala.util.parsing.input._
import scala.util._

object LaTeX extends AmidolParser {

  /** Parse out a simple latex equation from a string */
  val latexExpr: PackratParser[Expr[Double]] = {

    lazy val doubleAtom: PackratParser[Expr[Double]] =
      ( "-" ~> doubleAtom                ^^ { e => Negate(e) }
      | floatingPointNumber              ^^ { s => Literal(s.toDouble)  }
      | raw"(?U)\p{L}[\p{L}\p{No}_]*".r  ^^ { v => Variable(Symbol(v))  }
      | ( "\\alpha"
        | "\\beta"
        | "\\gamma"
        | "\\epsilon"
        )                                ^^ { v => Variable(Symbol(v))  }
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


