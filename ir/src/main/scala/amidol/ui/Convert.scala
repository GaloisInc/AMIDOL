package amidol.ui

import amidol._
import scala.util._

object convert {

  // Typeclass for converting into and out of UI types
  trait UiRepresentable[A] {
    // UI representation of A
    type UiRepr
  
    def fromUi(ui: UiRepr): Try[A]
    def toUi(a: A): UiRepr
  }
  object UiRepresentable {
    type Out[A, U] = UiRepresentable[A] { type UiRepr = U }
  }
  
  
  implicit object graphRepr extends UiRepresentable[amidol.Graph] {
    type UiRepr = ui.Graph

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

    def fromUi(g: ui.Graph): Try[amidol.Graph] = for {
      gNodes <- sequenceTry[amidol.Node](g.nodes.map(nodeRepr.fromUi(_)))
      gNodesMap = gNodes.map(n => n.id -> n).toMap
      gEdges <- sequenceTry[amidol.Edge](g.links.map(edgeRepr.fromUi(_)))
      gEdgesMap = gEdges.map(e => e.id -> e).toMap
    } yield amidol.Graph(gNodesMap, gEdgesMap)

    def toUi(g: amidol.Graph): ui.Graph = ui.Graph(
      g.nodes.values.map(nodeRepr.toUi(_)).toSeq,
      g.edges.values.map(edgeRepr.toUi(_)).toSeq
    )
  }

  implicit object nodeRepr extends UiRepresentable[amidol.Node] {
    type UiRepr = ui.Node

    def fromUi(n: ui.Node): Try[amidol.Node] = for {
        exp <- Math.Expr(n.label)
        nId = NodeId(n.id)
        nStateVar <- exp.asVariable
        nView = n.view
        nLocation = n.location
      } yield amidol.Node(nId, nStateVar, nView, nLocation)
    
    def toUi(n: amidol.Node): ui.Node = ui.Node(
        n.id.id,
        n.view,
        n.stateVariable.prettyPrint(),
        n.location
      )
  }

  implicit object edgeRepr extends UiRepresentable[amidol.Edge] {
    type UiRepr = ui.Link

    def fromUi(e: ui.Link): Try[amidol.Edge] = for {
        eLabel <- Math.Expr(e.label)
        eId = EdgeId(e.id)
        eSource = NodeId(e.source)
        eTarget = NodeId(e.target)
      } yield amidol.Edge(eId, eSource, eTarget, eLabel)

    def toUi(e: amidol.Edge): ui.Link = ui.Link(
        e.id.id,
        e.source.id,
        e.target.id,
        e.label.prettyPrint()
      )
  }

}
