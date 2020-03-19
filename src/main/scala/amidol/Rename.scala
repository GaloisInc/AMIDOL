package amidol

import scala.util.Random
import scala.collection.mutable
import amidol.math._


class Renamer[From, To](
  sanitize: From => To,
  freshen: To => To
) {
  val toMapping = mutable.Map.empty[From, To]
  val fromMapping = mutable.Map.empty[To, From]

  // Lookup a variable and returned the renamed variant
  def get(v: From): Option[To] = toMapping.get(v)
  
  // Lookup a variable and returned the renamed variant. If you don't find an existing
  // entry for this variable, create a new one. 
  def getOrInsert(v: From): To = this.get(v).getOrElse {
    val sanitized = sanitize(v)
    var tentative: To = sanitized
    while (fromMapping.contains(tentative)) {
      tentative = freshen(tentative)
    }
    fromMapping += (tentative -> v)
    toMapping += (v -> tentative)
    tentative
  }

  // Lookup what intial variable a now renamed variable corresponded to
  def reverseGet(v: To): Option[From] = fromMapping.get(v)
}

object Renamer {
  def filterAscii(): Renamer[Variable, Variable] = {
    def isSimpleAscii(c: Char): Boolean = 
      c == '_' ||
      ('0' <= c && c <= '9') ||
      ('a' <= c && c <= 'z') ||
      ('A' <= c && c <= 'Z')

    def sanitize(v: Variable): Variable = Variable(Symbol(
      v.s.name.map((c: Char) => if (isSimpleAscii(c)) c else 'u')
    ))

    def freshen(v: Variable): Variable = Variable(Symbol(
      v.s.name + Math.abs(Random.nextInt).toString
    ))

    new Renamer(sanitize, freshen)
  }
}
