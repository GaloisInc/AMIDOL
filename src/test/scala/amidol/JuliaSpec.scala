package amidol

import org.scalatest._
import scala.util._
import scala.io._

class JuliaSpec extends FlatSpec with Matchers {

  val source = Source.fromResource("julia_ast.txt").mkString

  "The Julia parser" should "parse a sample model file" in {
    JuliaSExpr(source).get
  }

}
