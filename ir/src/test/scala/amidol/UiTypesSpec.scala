package amidol

import scala.io.Source
import org.scalatest._
import scala.util._
import spray.json._

class UiTypesSpec extends FlatSpec with Matchers {

  "The backend" should "deserialize JSON satisfying the UI spec" in {
    val graphJsonSrc: String = Source.fromResource("sirs_graph.json").getLines.mkString
    val expectedGraph: Graph = Graph(
      nodes = Seq(
        Node(0,"susceptible_dummy.svg","Susceptible",Point(100,100)),
        Node(1,"infectious_dummy.svg","Infectious",Point(300,100)),
        Node(2,"recovered_dummy.svg","Recovered",Point(500,100))
      ),
      links = Seq(
        Link(-1,0,1,"β * Susceptible * Infectious / N"),
        Link(-2,1,2,"γ * Infectious"),
        Link(-3,2,0,"μ * Recovered")
      )
    )
    graphJsonSrc.parseJson.convertTo[Graph] shouldEqual expectedGraph
  }

}
