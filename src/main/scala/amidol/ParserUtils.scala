package amidol

import scala.util.parsing.combinator._
import scala.util.parsing.input._
import scala.util._

trait AmidolParser extends JavaTokenParsers with PackratParsers {
  protected def runParser[A](parser: PackratParser[A], input: String): Try[A] = parseAll(parser, input) match {
    case Success(matched, _) => scala.util.Success(matched)
    case Failure(msg, in) => scala.util.Failure(new Exception(s"Failed at ${in.pos}: $msg\n\n${in.pos.longString}"))
    case Error(msg, in) => scala.util.Failure(new Exception(s"Errored at ${in.pos}: $msg\n\n${in.pos.longString}"))
    case _ => scala.util.Failure(new Exception("Parser failed in an unexpected way"))
  }
}
