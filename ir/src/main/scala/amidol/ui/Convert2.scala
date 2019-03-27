package amidol.uinew

import amidol._
import scala.util._

object convert {

  implicit object graphRepr {
    type UiRepr = uinew.Graph

    private def sequenceTry[A](s: Seq[Try[A]]): Try[Seq[A]] = {
      val builder = Seq.newBuilder[A]
      
      for (tryA <- s) {
        tryA match {
          case Failure(f) => return Failure(f)
          case Success(a) => builder += a
        }
      }
    
      Success(builder.result)
    }

    def fromUi(g: uinew.Graph): Try[amidol.Model] = Try {
      val outgoingLinks = g.links.map(l => (l.from, NounId(l.to))).toMap
      val incomingLinks = g.links.map(l => (l.to, NounId(l.from))).toMap

      val nouns = List.newBuilder[Noun]
      val verbs = List.newBuilder[Verb]

      for (Node(id, image, label, props, x, y) <- g.nodes) {
        props match {
          case NounProps(params) => 
            // TODO do something with `params`
            nouns += Noun(NounId(id), math.Variable(Symbol(label)), image, ui.Point(x,y))
          case VerbProps(rate_template, params) =>
            // TODO do something with `params`
            verbs += Verb(VerbId(id), incomingLinks(id), outgoingLinks(id), math.Expr(rate_template).get)
        }
      }

      val inputVar = math.Variable('INPUT)
      val outputVar = math.Variable('OUTPUT)

      val nounMap = nouns.result.map(n => n.id -> n).toMap
      val verbMap = verbs.result.map((v: Verb) => {
        val substitution = Map(
          inputVar -> nounMap(v.source).stateVariable,
          outputVar -> nounMap(v.target).stateVariable
        )
        v.id -> v.copy(label = v.label.applySubstitution(substitution))
      }).toMap

      Model(nounMap, verbMap)
    }

    def toUi(g: amidol.Model): uinew.Graph = ???
  }
}
