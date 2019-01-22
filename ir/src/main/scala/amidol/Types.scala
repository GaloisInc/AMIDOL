package amidol

import amidol.Math._

import java.util.Date

// All of this file is subject to a ton of change! No idea what the best way is to handle this...

case class NodeId(id: Long) extends AnyVal
case class EdgeId(id: Long) extends AnyVal

case class Graph(
  nodes: Map[NodeId, Node],
  edges: Map[EdgeId, Edge]
)

case class Node(
  id: NodeId,
  stateVariable: Variable,
  
  // Only for pruposes of recreating ui.Graph. TODO: should we remove?
  view: String,
  location: ui.Point
)

case class Edge(
  id: EdgeId,
  source: NodeId, 
  target: NodeId,
  label: Expr
)


