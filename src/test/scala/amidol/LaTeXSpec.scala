package amidol

import amidol.math._
import org.scalatest._
import scala.util._

class LaTeXSpec extends FlatSpec with Matchers {

  "The latex parser" should "support numbers and variables" in {
    LaTeX("1").get       shouldEqual Literal(1)
    LaTeX("1.34e10").get shouldEqual Literal(1.34e10)
    LaTeX("x").get       shouldEqual Variable('x)
    LaTeX("foo").get     shouldEqual Variable('foo)
  }

  it should "parse subscripts, superscripts, and greek letters" in {
    LaTeX("β₂").get shouldEqual Variable(Symbol("β₂"))
  }

  it should "support compound operations" in {
    LaTeX("-x").get    shouldEqual Negate(Variable('x))
    LaTeX("1 + 2").get shouldEqual Plus(Literal(1), Literal(2))
    LaTeX("1 * 2").get shouldEqual Mult(Literal(1), Literal(2))
    LaTeX("1 - 2").get shouldEqual Plus(Literal(1), Negate(Literal(2)))
    LaTeX("1 / 2").get shouldEqual Mult(Literal(1), Inverse(Literal(2)))
  }

  it should "handle the usual precedence rules" in {
    LaTeX("1 + 2/5").get         shouldEqual Plus(1,Mult(2,Inverse(5)))
    LaTeX("1*(8 + x)/2 - 9").get shouldEqual Plus(Mult(Mult(1,Plus(8,'x)),Inverse(2)),Negate(9))
    LaTeX("1 - 2 - 3 + 4").get   shouldEqual Plus(Plus(Plus(1,Negate(2.0)),Negate(3.0)),4.0)
    LaTeX("-x/-(y + 1/3)").get   shouldEqual Mult(Negate('x),Inverse(Negate(Plus('y,Mult(1.0,Inverse(3.0))))))
  }

  it should "handle typical LaTeX" in {
    LaTeX("- \\frac{\\beta \\cdot S \\cdot I}{N}").get                   shouldEqual
      Negate(Mult(Mult(Mult(Variable(Symbol("\\beta")), Variable('S)), Variable('I)), Inverse(Variable('N))))

    LaTeX("\\frac{\\beta \\cdot S \\cdot I}{N} - \\gamma \\cdot I").get  shouldEqual
      Plus(
        Mult(Mult(Mult(Variable(Symbol("\\beta")), Variable('S)), Variable('I)), Inverse(Variable('N))),
        Negate(Mult(Variable(Symbol("\\gamma")), Variable('I)))
      )

    LaTeX("\\gamma \\cdot I").get                                        shouldEqual
      Mult(Variable(Symbol("\\gamma")), Variable('I))
  }

  it should "handle implicit multiplication" in {
    LaTeX("- \\frac{\\beta S I}{N}").get                   shouldEqual
      Negate(Mult(Mult(Mult(Variable(Symbol("\\beta")), Variable('S)), Variable('I)), Inverse(Variable('N))))

    LaTeX("\\frac{\\beta S I}{N} - \\gamma I").get  shouldEqual
      Plus(
        Mult(Mult(Mult(Variable(Symbol("\\beta")), Variable('S)), Variable('I)), Inverse(Variable('N))),
        Negate(Mult(Variable(Symbol("\\gamma")), Variable('I)))
      )

    LaTeX("\\gamma I").get                                        shouldEqual
      Mult(Variable(Symbol("\\gamma")), Variable('I))
  }
}
