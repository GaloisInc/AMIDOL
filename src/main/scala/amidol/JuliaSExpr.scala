package amidol

import scala.util.parsing.combinator._
import scala.util.parsing.input._
import scala.util._

/** Julia's `show_sexpr` outputs somewwhat non-standard looking SExpr:
 *
 *   - lists are comma-delimited
 *   - atoms can have (presumably only one set of) parens
 *   - literals are a bit confusing (the symbol literal, 'nothing', quote)
 *
 * Consequently, we have to roll our own parser. *Sigh*
 */
sealed trait JuliaSExpr
case class SNested(args: List[JuliaSExpr]) extends JuliaSExpr
case class SAtom(atom: String) extends JuliaSExpr
case class SSymbol(sym: String) extends JuliaSExpr
case class SNum(number: Double) extends JuliaSExpr
case class SQuote(quoted: JuliaSExpr) extends JuliaSExpr
case object SNothing extends JuliaSExpr

object JuliaSExpr extends AmidolParser {

  lazy val sexpr: PackratParser[JuliaSExpr] =
    ( ":" ~> raw"[^\s,\(\)]+".r         ^^ { a => SAtom(a) }          // symbol
    | ":" ~> raw"\([^\s,\(\)]+\)".r     ^^ { a => SAtom(a) }          // parenthesized symbol
    | raw"#QuoteNode".r ~> sexpr        ^^ { a => SQuote(a) }         // quoted node
    | "(" ~> repsep(sexpr, ",") <~ ")"  ^^ { a => SNested(a) }        // nested expression
    | "Symbol(" ~> stringLiteral <~ ")" ^^ { s => mkSSymbol(s) }      // Symbol literal
    | "nothing"                         ^^ { _ => SNothing }          // "nothing" literal
    | wholeNumber                       ^^ { n => SNum(n.toDouble) }  // number literal
    )

  private def mkSSymbol(rawStr: String): SSymbol =
    SSymbol(StringContext.treatEscapes(rawStr.tail.init))

  def apply(input: String): Try[JuliaSExpr] = runParser(sexpr, input)
}


