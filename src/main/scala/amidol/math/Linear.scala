package amidol.math

import scala.util.parsing.combinator._
import scala.util.parsing.input._
import scala.util._
import scala.collection.immutable.ArraySeq

// Dense representation of linear system
case class LinearSystem(
  variables: ArraySeq[Variable],      // dimension: N
  coefficients: ArraySeq[ArraySeq[Expr[Double]]] // dimensions: N x N
) {
  def dim: Int = variables.length
}

object Linear {

  /** Decompose an expression into its immediate terms
   *
   *  @param expr expressions whose terms we are extracting
   *  @returns list of the terms
   */
  def immediateTerms(expr: Expr[Double]): Seq[Expr[Double]] = expr match {
    case Plus(x, y) => immediateTerms(x) ++ immediateTerms(y)
    case factor => Seq(factor)
  }

  /** Decompose an expression into its immediate factors
   *
   *  @param expr expressions whose factors we are extracting
   *  @returns list of the factors
   */
  def immediateFactors(expr: Expr[Double]): Seq[Expr[Double]] = expr match {
    case Negate(x) => Literal(-1.0) +: immediateFactors(x)
    case Mult(x, y) => immediateFactors(x) ++ immediateFactors(y)
    case atom => Seq(atom)
  }

  // TODO: make this try to normalize the system before giving up
  def fromEquations(equations: Map[Variable, Expr[Double]]): Option[LinearSystem] = {

    // Checks if the expression contains any variables that are not constants
    def isConstant: Expr[Double] => Boolean = {
      case Plus(lhs, rhs) => isConstant(lhs) && isConstant(rhs)
      case Mult(lhs, rhs) => isConstant(lhs) && isConstant(rhs)
      case Negate(e) => isConstant(e)
      case Inverse(e) => isConstant(e)
      case v: Variable => !equations.contains(v)
      case Sin(e) => isConstant(e)
      case Max(lhs, rhs) => isConstant(lhs) && isConstant(rhs)
      case _: Literal[Double] => true
    }

    // Linear equations
    val decomposed: Map[Variable, Map[Variable, Expr[Double]]] =
      equations.view
        .mapValues { case expr =>
          val linearTerms: Seq[(Variable, Expr[Double])] = for {
            term <- immediateTerms(expr)
            factors = immediateFactors(term)
            (theVars, theRest) = factors.partition {
              case x if isConstant(x) => false
              case v: Variable => true
              case _ => return None
            }
            theVar = theVars match {
              case List(v: Variable) => v
              case _ => return None
            }
          } yield (theVar, theRest.foldLeft[Expr[Double]](1)(Mult(_,_)))

          linearTerms.toMap
        }
        .toMap

    // Pick an order for variables and make the matrix
    val variables: ArraySeq[Variable] = equations.keys.to(ArraySeq)
    val dim = variables.length
    val coefficients: ArraySeq[ArraySeq[Expr[Double]]] = ArraySeq.tabulate(dim, dim) { (i: Int, j: Int) =>
      decomposed
        .getOrElse(variables(i), Map())
        .getOrElse(variables(j), 0)
   }

    Some(LinearSystem(variables, coefficients))
  }

}
