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
      val outgoingLinks = graph.links.values.map(l => (l.from, StateId(l.to))).toMap
      val incomingLinks = graph.links.values.map(l => (l.to, StateId(l.from))).toMap

      val states = List.newBuilder[State]
      val events = List.newBuilder[Event]
      val initialConditions = Map.newBuilder[String, Double]

      for (Node(id, image, label, props, x, y) <- graph.nodes.values) {
        props match {
          case NounProps(params) =>
            val paramMap = params.map { case Parameter(n,v) => n -> v.toDouble }.toMap
            initialConditions += (label -> paramMap("Initial"))
            states += State(StateId(id), math.Variable(Symbol(label)), image, ui.Point(x,y))
          case VerbProps(verb_sort, params) =>
            val paramMap = params
              .map { case Parameter(n,v) => math.Variable(Symbol(n)) -> math.Expr(v).get }
              .toMap
            events += (verb_sort match {
              case Conserved(rate_template) =>
                val rateExpr = math.Expr(rate_template).get.applySubstitution(paramMap)
                amidol.Conserved(EventId(id), incomingLinks(id), outgoingLinks(id), rateExpr)
              case Unconserved(rate_in_template, rate_out_template) =>
                val rateInExpr = math.Expr(rate_in_template).get.applySubstitution(paramMap)
                val rateOutExpr = math.Expr(rate_out_template).get.applySubstitution(paramMap)
                amidol.Unconserved(EventId(id), incomingLinks(id), outgoingLinks(id), rateOutExpr, rateInExpr)
              case Source(rate_in_template) =>
                val rateInExpr = math.Expr(rate_in_template).get.applySubstitution(paramMap)
                amidol.Source(EventId(id), outgoingLinks(id), rateInExpr)
              case Sink(rate_out_template) =>
                val rateOutExpr = math.Expr(rate_out_template).get.applySubstitution(paramMap)
                amidol.Sink(EventId(id), incomingLinks(id), rateOutExpr)
            })
        }
      }

      val inputVar = math.Variable('INPUT)
      val outputVar = math.Variable('OUTPUT)

      val stateMap = states.result.map(n => n.id -> n).toMap
      val eventMap = events.result.map {
        case v: amidol.Conserved =>
          val substitution = Map(
            inputVar -> stateMap(v.source).stateVariable,
            outputVar -> stateMap(v.target).stateVariable
          )
          v.id -> v.copy(
            rate = v.rate.applySubstitution(substitution)
          )

        case v: amidol.Unconserved =>
          val substitution = Map(
            inputVar -> stateMap(v.source).stateVariable,
            outputVar -> stateMap(v.target).stateVariable
          )
          v.id -> v.copy(
            rateOut = v.rateOut.applySubstitution(substitution),
            rateIn = v.rateIn.applySubstitution(substitution)
          )

        case v: amidol.Source =>
          val substitution = Map(
            outputVar -> stateMap(v.target).stateVariable
          )
          v.id -> v.copy(
            rateIn = v.rateIn.applySubstitution(substitution)
          )

        case v: amidol.Sink =>
          val substitution = Map(
            inputVar -> stateMap(v.source).stateVariable,
          )
          v.id -> v.copy(
            rateOut = v.rateOut.applySubstitution(substitution)
          )
      }.toMap

      (Model(stateMap, eventMap), initialConditions.result)
    }

    def toUi(g: amidol.Model): uinew.Graph = ???
  }
}
