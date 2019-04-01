package amidol.ui

import amidol._
import scala.util.Try
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

/// The full set of shapes and lines in the UI
case class Graph(
  nodes: Map[String,Node],
  links: Map[String,Link]
) {
  def parse(): Try[(amidol.Model, Map[String, Double])] = Try {
    val outgoingLinks = links.values.map(l => (l.from, StateId(l.to))).toMap
    val incomingLinks = links.values.map(l => (l.to, StateId(l.from))).toMap

    val states = List.newBuilder[State]
    val events = List.newBuilder[Event]
    val initialConditions = Map.newBuilder[String, Double]

    for (Node(id, image, label, props, x, y) <- nodes.values) {
      props match {
        case NounProps(params) =>
          val paramMap = params.map { case Parameter(n,v) => n -> v.toDouble }.toMap
          initialConditions += (label -> paramMap("Initial"))
          states += State(StateId(id), math.Variable(Symbol(label)))
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
}
object Graph extends UiJsonSupport

/// A single shape in the UI
case class Node(
  id: String,
  image: String,
  label: String,
  props: NodeProps,
  x: Long,
  y: Long
)
object Node extends UiJsonSupport

sealed trait NodeProps
object NodeProps extends UiJsonSupport

case class VerbProps(
  verb_sort: VerbSort,
  parameters: Seq[Parameter]
) extends NodeProps
object VerbProps extends UiJsonSupport

case class NounProps(
  parameters: Seq[Parameter]
) extends NodeProps
object NounProps extends UiJsonSupport

case class Parameter(
  name: String,
  value: String
)
object Parameter extends UiJsonSupport

sealed trait VerbSort

case class Conserved(
  rate_template: String
) extends VerbSort
object Conserved extends UiJsonSupport

case class Unconserved(
  rate_in_template: String,
  rate_out_template: String
) extends VerbSort
object Unconserved extends UiJsonSupport

case class Source(
  rate_in_template: String
) extends VerbSort
object Source extends UiJsonSupport

case class Sink(
  rate_out_template: String
) extends VerbSort
object Sink extends UiJsonSupport

/// And edge between two nodes in the UI
case class Link(
  id: String,
  from: String,
  to: String
)
object Link extends UiJsonSupport

trait UiJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val parameterFormat = jsonFormat2(Parameter.apply)
  implicit val nounFormat = jsonFormat1(NounProps.apply)

  implicit val conservedFormat = jsonFormat1(Conserved.apply)
  implicit val unconservedFormat = jsonFormat2(Unconserved.apply)
  implicit val sourceFormat = jsonFormat1(Source.apply)
  implicit val sinkFormat = jsonFormat1(Sink.apply)
   
  implicit object verbSortFormat extends RootJsonFormat[VerbSort] {
    def read(v: JsValue): VerbSort = {
      v.asJsObject.fields("type").convertTo[String] match {
        case "conserved" => conservedFormat.read(v)
        case "unconserved" => unconservedFormat.read(v)
        case "source" => sourceFormat.read(v)
        case "sink" => sinkFormat.read(v)
      }
    }
    def write(p: VerbSort): JsValue = p match {
      case v: Conserved => conservedFormat.write(v)
      case v: Unconserved => unconservedFormat.write(v)
      case v: Source => sourceFormat.write(v)
      case v: Sink => sinkFormat.write(v)
    }
  }
  
  implicit val verbFormat = jsonFormat2(VerbProps.apply)

  implicit object propFormat extends RootJsonFormat[NodeProps] {
    def read(v: JsValue): NodeProps = {
      v.asJsObject.fields("type").convertTo[String] match {
        case "verb" => verbFormat.read(v)
        case "noun" => nounFormat.read(v)
      }
    }
    def write(p: NodeProps): JsValue = p match {
      case n: NounProps => JsObject(nounFormat.write(n).asJsObject.fields + ("type" -> JsString("noun")))
      case v: VerbProps => JsObject(verbFormat.write(v).asJsObject.fields + ("type" -> JsString("verb")))
    }
  }

  implicit val linkFormat = jsonFormat3(Link.apply)
  implicit val nodeFormat = jsonFormat6(Node.apply)
  implicit val graphFormat = jsonFormat2(Graph.apply)
}

