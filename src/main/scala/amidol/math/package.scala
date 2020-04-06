package amidol

import scala.util.parsing.combinator._
import scala.util.parsing.input._
import scala.util._
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

package object math {


  implicit class ExprOps(expr: Expr[_]) {

    // Turn into a  pretty string representation.
    //
    // Compatible with Python's syntax
    def prettyPrint(precedence: Int = 0): String = {
      val p = expr.precedence
      def wrap(s: String): String = if (precedence > expr.precedence) { "(" + s + ")" } else { s }
      expr match {
        case Or(l,r)            => wrap(l.prettyPrint(p) + " or " + r.prettyPrint(p+1))
        case And(l,r)           => wrap(l.prettyPrint(p) + " and " + r.prettyPrint(p+1))
        case Comparision(l,GT,r)=> wrap(l.prettyPrint(p) + " > " + r.prettyPrint(p+1))
        case Comparision(l,EQ,r)=> wrap(l.prettyPrint(p) + " == " + r.prettyPrint(p+1))
        case Comparision(l,LT,r)=> wrap(l.prettyPrint(p) + " < " + r.prettyPrint(p+1))
        case Plus(l,Negate(r))  => wrap(l.prettyPrint(p) + " - " + r.prettyPrint(p+1))
        case Plus(l,r)          => wrap(l.prettyPrint(p) + " + " + r.prettyPrint(p+1))
        case Mult(l,Inverse(r)) => wrap(l.prettyPrint(p) + " / " + r.prettyPrint(p+1))
        case Mult(l,r)          => wrap(l.prettyPrint(p) + " * " + r.prettyPrint(p+1))
        case Negate(x)          => wrap("-" + x.prettyPrint(p))
        case Not(x)             => wrap("not " + x.prettyPrint(p))
        case x: Inverse         => Mult(Literal(1), x).prettyPrint(precedence)
        case Variable(s)        => s.name
        case Literal(d)         => d.toString
        case Sin(x)             => wrap("sin(" + x.prettyPrint(0) + ")")
        case Max(l,r)           => wrap("max(" + l.prettyPrint(0) + "," + r.prettyPrint(0) + ")")
        case DataSeries(n)      => wrap("dataSeries(" + n + ")")
      }
    }

    def dataSeries(): Set[DataSeries] = {
      val series = Set.newBuilder[DataSeries]

      // Make a visitor which adds to the data series accumulator
      val seriesVisitor = new ExprVisitor {
        override def visitDataSeries(s: DataSeries) = series += s
      }
      seriesVisitor.visit(expr)

      series.result()
    }

    def variables(): Set[Variable] = {
      val variables = Set.newBuilder[Variable]

      // Make a visitor which adds to the variable accumulator
      val variableVisitor = new ExprVisitor {
        override def visitVariable(v: Variable) = variables += v
      }
      variableVisitor.visit(expr)

      variables.result()
    }

  }

  implicit object arithmeticSupport extends RootJsonFormat[Expr[Double]] {
    def write(e: Expr[Double]) = JsString(e.prettyPrint())
    def read(json: JsValue) = json match {
      case JsNumber(v) => Literal(v.toDouble)
      case JsString(s) =>
        Expr.expression(s) match {
          case Success(s) => s
          case Failure(f) => throw DeserializationException("Invalid arithmetic equation: " + f.toString)
        }
      case _ => throw DeserializationException("Invalid arithmetic format: " + json)
    }
  }
  implicit object predicateSupport extends RootJsonFormat[Expr[Boolean]] {
    def write(e: Expr[Boolean]) = JsString(e.prettyPrint())
    def read(json: JsValue) = json match {
      case JsTrue => Literal(true)
      case JsFalse => Literal(false)
      case JsString(s) =>
        Expr.predicate(s) match {
          case Success(s) => s
          case Failure(f) => throw DeserializationException("Invalid predicate equation: " + f.toString)
        }
      case _ => throw DeserializationException("Invalid predicate format: " + json)
    }
  }

}
