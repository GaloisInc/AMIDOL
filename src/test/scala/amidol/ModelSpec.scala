package amidol

import org.scalatest._
import scala.util._
import scala.io._
import spray.json._

class ModelSpec extends FlatSpec with Matchers {

  val source = Source.fromResource("sirs_model.json").mkString

  "The model parser" should "parse its JSON representation" in {
    source.parseJson.convertTo[Model]
  }
}
