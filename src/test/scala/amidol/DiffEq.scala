package amidol

import amidol.math._
import org.scalatest._
import scala.util._

class DiffEqSpec extends FlatSpec with Matchers {

  "The latex diff-eq extractor" should "extract derivatives" in {
    LatexExtract.extractDerivative(LaTeX("\\frac{dX}{dt}").get).get             shouldEqual Variable(Symbol("X"))
    LatexExtract.extractDerivative(LaTeX("\\frac{d X}{dt}").get).get            shouldEqual Variable(Symbol("X"))
    LatexExtract.extractDerivative(LaTeX("\\frac{d\\gamma}{dt}").get).get        shouldEqual Variable(Symbol("\\gamma"))
    LatexExtract.extractDerivative(LaTeX("\\frac{dSusceptible}{d t}").get).get  shouldEqual Variable(Symbol("Susceptible"))
    LatexExtract.extractDerivative(LaTeX("\\frac{d Susceptible}{d t}").get).get shouldEqual Variable(Symbol("Susceptible"))
  }

  it should "extract initial conditions" in {
    LatexExtract.extractInitialCond(LaTeX("X(0)").get).get           shouldEqual Variable(Symbol("X"))
    LatexExtract.extractInitialCond(LaTeX("\\gamma(0)").get).get     shouldEqual Variable(Symbol("\\gamma"))
    LatexExtract.extractInitialCond(LaTeX("Susceptible(0)").get).get shouldEqual Variable(Symbol("Susceptible"))
  }

  it should "parse out a full model" in {
    LatexExtract.extractFromSource(List(
      "\\frac{dS}{dt} = - \\frac{\\beta S I}{N}",
      "\\frac{dI}{dt} = \\frac{\\beta S I}{N} - \\gamma I",
      "\\beta = 0.3",
      "\\gamma = 0.33",
      "I(0) = 1",
      "S(0) = N - 1",
      "\\frac{dR}{dt} = \\gamma I",
      "N = 300"
    )).get shouldEqual
    Model(
      Map(
        StateId("S") -> State(Variable(Symbol("S")),None,Plus(Variable('N),Negate(Literal(1.0)))),
        StateId("I") -> State(Variable(Symbol("I")),None,Literal(1.0)),
        StateId("R") -> State(Variable(Symbol("R")),None,Literal(0.0))
      ),
      Map(
        EventId("event_1") -> Event(
          Negate(Mult(Mult(Mult(Variable(Symbol("\\beta")),Variable('S)),Variable('I)),Inverse(Variable('N)))),
          None,
          OutputPredicate(Map(StateId("S") -> Literal(1.0))),
          None
        ),
        EventId("event_2") -> Event(
          Plus(Mult(Mult(Mult(Variable(Symbol("\\beta")),Variable('S)),Variable('I)),Inverse(Variable('N))),Negate(Mult(Variable(Symbol("\\gamma")),Variable('I)))),
          None,
          OutputPredicate(Map(StateId("I") -> Literal(1.0))),
          None
        ),
        EventId("event_3") -> Event(
          Mult(Variable(Symbol("\\gamma")),Variable('I)),
          None,
          OutputPredicate(Map(StateId("R") -> Literal(1.0))),
          None
        )
      ),
      Map(
        Variable(Symbol("N")) -> 300.0,
        Variable(Symbol("\\beta")) -> 0.3,
        Variable(Symbol("\\gamma")) -> 0.33
      )
    )
  }
}
