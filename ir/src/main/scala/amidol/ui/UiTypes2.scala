package amidol.uinew

import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

/// The full set of shapes and lines in the UI
case class Graph(
  nodes: Map[String,Node],
  links: Map[String,Link]
)
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

