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

    def fromUi(graph: uinew.Graph): Try[(amidol.Model, Map[String, Double])] = Try {
      val outgoingLinks = graph.links.values.map(l => (l.from, NounId(l.to))).toMap
      val incomingLinks = graph.links.values.map(l => (l.to, NounId(l.from))).toMap

      val nouns = List.newBuilder[Noun]
      val verbs = List.newBuilder[Conserved]
      val initialConditions = Map.newBuilder[String, Double]

      for (Node(id, image, label, props, x, y) <- graph.nodes.values) {
        props match {
          case NounProps(params) =>
            val paramMap = params.map { case Parameter(n,v) => n -> v.toDouble }.toMap
            initialConditions += (label -> paramMap("Initial"))
            nouns += Noun(NounId(id), math.Variable(Symbol(label)), image, ui.Point(x,y))
          case VerbProps(rate_template, params) =>
            val paramMap = params
              .map { case Parameter(n,v) => math.Variable(Symbol(n)) -> math.Literal(v.toDouble) }
              .toMap
            val rateExpr = math.Expr(rate_template).get.applySubstitution(paramMap)
            verbs += Conserved(VerbId(id), incomingLinks(id), outgoingLinks(id), rateExpr)
        }
      }

      val inputVar = math.Variable('INPUT)
      val outputVar = math.Variable('OUTPUT)

      val nounMap = nouns.result.map(n => n.id -> n).toMap
      val verbMap = verbs.result.map((v: Conserved) => {
        val substitution = Map(
          inputVar -> nounMap(v.source).stateVariable,
          outputVar -> nounMap(v.target).stateVariable
        )
        v.id -> v.copy(rate = v.rate.applySubstitution(substitution))
      }).toMap

      (Model(nounMap, verbMap), initialConditions.result)
    }

    def toUi(g: amidol.Model): uinew.Graph = ???
  }
}
