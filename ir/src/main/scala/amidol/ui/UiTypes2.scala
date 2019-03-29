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
  rate_template: String,
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

