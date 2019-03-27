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
  
  
  implicit object graphRepr extends UiRepresentable[amidol.Model] {
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

    def fromUi(g: ui.Graph): Try[amidol.Model] = for {
      gNodes <- sequenceTry[amidol.Noun](g.nodes.map(nodeRepr.fromUi(_)))
      gNouns = gNodes.map(n => n.id -> n).toMap
      gEdges <- sequenceTry[Verb](g.links.map(edgeRepr.fromUi(_)))
      gVerbs = gEdges.map(e => e.id -> e).toMap
    } yield amidol.Model(gNouns, gVerbs)

    def toUi(g: amidol.Model): ui.Graph = ui.Graph(
      g.nouns.values.map(nodeRepr.toUi(_)).toSeq,
      g.verbs.values.map(edgeRepr.toUi(_)).toSeq
    )
  }

  implicit object nodeRepr extends UiRepresentable[amidol.Noun] {
    type UiRepr = ui.Node

    def fromUi(n: ui.Node): Try[amidol.Noun] = for {
        exp <- math.Expr(n.label)
        nId = NounId(n.id)
        nStateVar <- exp.asVariable
        nView = n.view
        nLocation = n.location
      } yield amidol.Noun(nId, nStateVar, nView, nLocation)
    
    def toUi(n: amidol.Noun): ui.Node = ui.Node(
        n.id.id,
        n.view,
        n.stateVariable.prettyPrint(),
        n.location
      )
  }

  implicit object edgeRepr extends UiRepresentable[Verb] {
    type UiRepr = ui.Link

    def fromUi(e: ui.Link): Try[Verb] = for {
        eLabel <- math.Expr(e.label)
        eId = VerbId(e.id)
        eSource = NounId(e.source)
        eTarget = NounId(e.target)
      } yield Verb(eId, eSource, eTarget, eLabel)

    def toUi(e: Verb): ui.Link = ui.Link(
        e.id.id,
        e.source.id,
        e.target.id,
        e.label.prettyPrint()
      )
  }

}
