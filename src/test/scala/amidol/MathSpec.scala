package amidol

import amidol.math._
import org.scalatest._
import scala.util._

class MathSpec extends FlatSpec with Matchers {

  "The expression parser" should "support numbers and variables" in {
    Expr("1").get       shouldEqual Literal(1)
    Expr("1.34e10").get shouldEqual Literal(1.34e10)
    Expr("x").get       shouldEqual Variable('x)
    Expr("foo").get     shouldEqual Variable('foo)
  }

  it should "support compound operations" in {
    Expr("-x").get    shouldEqual Negate(Variable('x))
    Expr("1 + 2").get shouldEqual Plus(Literal(1), Literal(2))
    Expr("1 * 2").get shouldEqual Mult(Literal(1), Literal(2))
    Expr("1 - 2").get shouldEqual Plus(Literal(1), Negate(Literal(2)))
    Expr("1 / 2").get shouldEqual Mult(Literal(1), Inverse(Literal(2)))
  }

  it should "handle the usual precedence rules" in {
    Expr("1 + 2/5").get         shouldEqual Plus(1,Mult(2,Inverse(5)))
    Expr("1*(8 + x)/2 - 9").get shouldEqual Plus(Mult(Mult(1,Plus(8,'x)),Inverse(2)),Negate(9))
    Expr("1 - 2 - 3 + 4").get   shouldEqual Plus(Plus(Plus(1,Negate(2.0)),Negate(3.0)),4.0)
    Expr("-x/-(y + 1/3)").get   shouldEqual Mult(Negate('x),Inverse(Negate(Plus('y,Mult(1.0,Inverse(3.0))))))
  }


  "Expressions" should "evaluate correctly" in {
    Expr("--1").get.eval()                                     shouldEqual 1
    Expr("1 + 2/5").get.eval()                                 shouldEqual 1.4 
    Expr("1*(8 + 9)/2 - 9").get.eval()                         shouldEqual (-0.5)
    Expr("1 - 2 - 3 + 4").get.eval()                           shouldEqual 0
    Expr("-x/-(y + 1/4)").get.eval(Map('x -> 1.5, 'y -> 0.75)) shouldEqual 1.5 
  }
}
