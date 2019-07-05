package amidol

import org.scalatest._
import scala.util._
import scala.io._
import spray.json._

class JuliaSpec extends FlatSpec with Matchers {

  val source = Source.fromResource("julia_ast.txt").mkString
  var parsed: JuliaSExpr = null
  var extracted: Model = null

  "The Julia parser" should "parse a sample model file" in {
    parsed = JuliaSExpr(source).get
  }
  
  it should "then extract that result into an AMIDOL model" in {
    extracted = parsed.extractModel()
  }

  "The final model" should "be serializable/deserializable to/from JSON" in {
    val json = extracted.toJson
    val parsedModel = json.convertTo[Model]
    parsedModel shouldEqual extracted
  }
}
