package amidol.ui

import amidol._
import scala.util.{Failure, Success, Try}
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import collection.mutable.{HashMap, MultiMap, Set}

/// The full set of shapes and lines in the UI
case class Graph(
  nodes: Map[String,Node],
  links: Map[String,Link]
)  {
  def parse(paletteModels: Map[String, amidol.PaletteItem]): Try[amidol.Model] = Try {
    val nounIds = nodes.keys.zipWithIndex.toMap

    var outgoingLinks = new HashMap[String, Set[String]] with MultiMap[String, String]
    var incomingLinks = new HashMap[String, Set[String]] with MultiMap[String, String]

    links.values.foreach { case Link(_, from, to) =>
      outgoingLinks.addBinding(from, to)
      incomingLinks.addBinding(to, from)
    }

    def getOutgoing(s: String): String = {
      outgoingLinks.getOrElse(s, Set.empty).toList match {
        case List(o) => o
        case Nil => throw new Exception(s"No outgoing edge from '${nodes.get(s).map(_.id).getOrElse(s)}' verb")
        case _ => throw new Exception(s"More than one outgoing edge from '${nodes.get(s).map(_.id).getOrElse(s)}' verb")
      }
    }
    def getIncoming(s: String): String = {
      incomingLinks.getOrElse(s, Set.empty).toList match {
        case List(o) => o
        case Nil => throw new Exception(s"No incoming edge to '${nodes.get(s).map(_.id).getOrElse(s)}' verb")
        case _ => throw new Exception(s"More than one incoming edge to '${nodes.get(s).map(_.id).getOrElse(s)}' verb")
      }
    }

    val verbs = Map.newBuilder[String, Model]
    val nouns = Map.newBuilder[String, Model]
    val shared = List.newBuilder[((String, StateId), (String, StateId))]
    val variableRename = Map.newBuilder[String, String]

    for (Node(id, label, props, x, y) <- nodes.values) {
      val paletteModel = paletteModels.getOrElse(
        props.className,
        throw new Exception(s"No model called '${props.className}' found in palette")
      )

      val consts = props.parameters
        .map { case Parameter(n,v) => math.Variable(Symbol(n)) -> v.asConstant.get.d }
        .toMap

      // When we have a noun, try to give the state variable a good name
      if (paletteModel.`type` == "noun" && paletteModel.sharedStates.length > 0 && paletteModel.sharedStates.distinct.length == 1) {
        variableRename += s"n${nounIds(id)}_${paletteModel.sharedStates(0).id}" -> label

        nouns += ("n" + nounIds(id)) -> paletteModel.backingModel.copy(constants = paletteModel.backingModel.constants ++ consts)
      } else {
        verbs += ("n" + nounIds(id)) -> paletteModel.backingModel.copy(constants = paletteModel.backingModel.constants ++ consts)
      }

      if (paletteModel.`type` == "verb") {

        val nounFrom = getIncoming(id)

        shared += ((("n" + nounIds(id), paletteModel.sharedStates(0)), ("n" + nounIds(nounFrom), paletteModels(nodes(nounFrom).props.className).sharedStates(1))))

        val nounTo = getOutgoing(id)
        shared += ((("n" + nounIds(id), paletteModel.sharedStates(1)), ("n" + nounIds(nounTo), paletteModels(nodes(nounTo).props.className).sharedStates(0))))
      }
    }

    val rename = variableRename.result().map { case (k,v) => math.Variable(Symbol(k)) -> math.Variable(Symbol(v)) }

    amidol.Model
      .composeModels(verbs.result().toList ++ nouns.result().toList, shared.result())
      .mapIds(identity, identity, v => rename.getOrElse(v,v))
  }
}
object Graph extends UiJsonSupport

/** A single shape in the UI
 *
 *  @param label what the user calls this instance (important so that we
 *               properly rename our states internally)
 */
case class Node(
  id: String,
  label: String,
  props: NodeProps,
  x: Long,
  y: Long
)
object Node extends UiJsonSupport

case class NodeProps (
  className: String,            // name of model in palette
  parameters:  Seq[Parameter],
)
object NodeProps extends UiJsonSupport

case class Parameter(
  name: String,
  value: math.Expr[Double],
)
object Parameter extends UiJsonSupport

/// And edge between two nodes in the UI
case class Link(
  id: String,
  from: String,
  to: String
)
object Link extends UiJsonSupport

/*
case class PaletteItem(
  className: String,           // this is how we link to the backend
  `type`: String,              // do we need this???
  sharedStates: Array[String], // currently not needed, but perhaps UI cares?
  icon: String,                // for display purposes only
  parameters: Seq[Parameter],  // defaulted off the backing model
)
object PaletteItem extends UiJsonSupport {
  def fromBackend(p: amidol.PaletteItem): PaletteItem = PaletteItem(
    p.className,
    p.`type`,
    p.sharedStates.map(_.id),
    p.icon,
    p.backingModel.constants
      .map { case (v, d) => Parameter(v.s.name, math.Literal(d)) }
      .toSeq,
  )
}
*/

trait UiJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val parameterFormat = jsonFormat2(Parameter.apply)
  implicit val nodePropsFormat = jsonFormat2(NodeProps.apply)
  implicit val linkFormat = jsonFormat3(Link.apply)
  implicit val nodeFormat = jsonFormat5(Node.apply)
  implicit val graphFormat = jsonFormat2(Graph.apply)
//  implicit val paletteFormat = jsonFormat5(PaletteItem.apply)
}

