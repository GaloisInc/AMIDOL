package amidol

import amidol.ui.convert._
import scala.io.Source
import org.scalatest._
import scala.util._
import spray.json._

class UiTypesSpec extends FlatSpec with Matchers {

  val graphJsonSrc: String = Source.fromResource("sirs_graph.json").getLines.mkString
  val expectedGraph: ui.Graph = ui.Graph(
    nodes = Seq(
      ui.Node(0, "susceptible_dummy.svg", "Susceptible", ui.Point(100,100)),
      ui.Node(1, "infectious_dummy.svg",  "Infectious",  ui.Point(300,100)),
      ui.Node(2, "recovered_dummy.svg",   "Recovered",   ui.Point(500,100))
    ),
    links = Seq(
      ui.Link(-1, 0, 1, "β * Susceptible * Infectious / N"),
      ui.Link(-2, 1, 2, "γ * Infectious"),
      ui.Link(-3, 2, 0, "μ * Recovered")
    )
  )

  "The backend" should "deserialize JSON satisfying the UI spec" in {
    graphJsonSrc.parseJson.convertTo[ui.Graph] shouldEqual expectedGraph
  }

  it should "successfully roundtrip a ui.Graph" in {
    graphRepr.toUi(graphRepr.fromUi(expectedGraph).get) shouldEqual expectedGraph
  }
}
