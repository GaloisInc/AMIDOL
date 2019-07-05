package amidol

import org.scalatest._
import scala.util._
import scala.io._

class JuliaSpec extends FlatSpec with Matchers {

  val source = Source.fromResource("julia_ast.txt").mkString
  var parsed: JuliaSExpr = null

  "The Julia parser" should "parse a sample model file" in {
    parsed = JuliaSExpr(source).get
  }
  
  it should "then extract that result into an AMIDOL model" in {
    (parsed).extractModel()
  }
}
