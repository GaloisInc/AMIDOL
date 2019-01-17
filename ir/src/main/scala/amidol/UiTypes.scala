package amidol

import spray.json._

/// The full set of shapes and lines in the UI
case class Graph(
  nodes: Seq[Node],
  links: Seq[Link]
)
object Graph extends DefaultJsonProtocol {
  implicit val graphFormat: JsonFormat[Graph] = jsonFormat2(Graph.apply)
}

/// A single shape in the UI
case class Node(
  id: Long,
  view: String,
  label: String,
  location: Point
)
object Node extends DefaultJsonProtocol {
  implicit val nodeFormat: JsonFormat[Node] = jsonFormat4(Node.apply)
}

/// A location in the UI
case class Point(
  x: Long,
  y: Long
)
object Point extends DefaultJsonProtocol {
  implicit val pointFormat: JsonFormat[Point] = jsonFormat2(Point.apply)
}

/// And edge between two nodes in the UI
case class Link(
  id: Long,
  source: Long,
  target: Long,
  label: String
)
object Link extends DefaultJsonProtocol {
  implicit val linkFormat: JsonFormat[Link] = jsonFormat4(Link.apply)
}
