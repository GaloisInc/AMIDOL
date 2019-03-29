package amidol.ui

import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

/// The full set of shapes and lines in the UI
case class Graph(
  nodes: Seq[Node],
  links: Seq[Link]
)
object Graph extends UiJsonSupport

/// A single shape in the UI
case class Node(
  id: String,
  view: String,
  label: String,
  location: Point
)
object Node extends UiJsonSupport

/// A location in the UI
case class Point(
  x: Long,
  y: Long
)
object Point extends UiJsonSupport

/// And edge between two nodes in the UI
case class Link(
  id: String,
  source: String,
  target: String,
  label: String
)
object Link extends UiJsonSupport

trait UiJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val pointFormat = jsonFormat2(Point.apply)
  implicit val linkFormat = jsonFormat4(Link.apply)
  implicit val nodeFormat = jsonFormat4(Node.apply)
  implicit val graphFormat = jsonFormat2(Graph.apply)
}

