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
  def parse(paletteModels: Map[String, amidol.Model]): Try[amidol.Model] = Try {
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

    for (Node(id, image, label, props, x, y) <- nodes.values) {
      val paletteModel = paletteModels.getOrElse(
        props.className,
        throw new Exception(s"No model called '${props.className}' found in palette")
      )

      val constants = props.parameters
        .map { case Parameter(n,v) => math.Variable(Symbol(n)) -> v.asConstant.get.d }
        .toMap

      // When we have a noun, try to give the state variable a good name
      if (props.`type` == "noun" && props.sharedStates.length > 0 && props.sharedStates.distinct.length == 1) {
        variableRename += s"n${nounIds(id)}_${props.sharedStates(0)}" -> label
      
        nouns += ("n" + nounIds(id)) -> paletteModel.copy(constants = paletteModel.constants ++ constants)
      } else {
        verbs += ("n" + nounIds(id)) -> paletteModel.copy(constants = paletteModel.constants ++ constants)
      }
     
      if (props.`type` == "verb") {
        
        val nounFrom = getIncoming(id)
        shared += ((("n" + nounIds(id), amidol.StateId(props.sharedStates(0))), ("n" + nounIds(nounFrom), amidol.StateId(nodes(nounFrom).props.sharedStates(1)))))
        
        val nounTo = getOutgoing(id)
        shared += ((("n" + nounIds(id), amidol.StateId(props.sharedStates(1))), ("n" + nounIds(nounTo), amidol.StateId(nodes(nounTo).props.sharedStates(0)))))
      }
    }

    val rename = variableRename.result().map { case (k,v) => math.Variable(Symbol(k)) -> math.Variable(Symbol(v)) }

    amidol.Model
      .composeModels(verbs.result().toList ++ nouns.result().toList, shared.result())
      .mapIds(identity, identity, v => rename.getOrElse(v,v))
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

case class NodeProps (
  className: String,            // name of model in palette
  classDef: String,             // name of model in palette
  parameters:  Seq[Parameter],
  `type`: String,              // verb or noun
  sharedStates: Array[String]  // For now, this is be: `[ <shared state for incoming arrows>
                               //                       , <shared state for outgoing arrows> ]
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

trait UiJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val parameterFormat = jsonFormat2(Parameter.apply)
  implicit val nodePropsFormat = jsonFormat5(NodeProps.apply)
  implicit val linkFormat = jsonFormat3(Link.apply)
  implicit val nodeFormat = jsonFormat6(Node.apply)
  implicit val graphFormat = jsonFormat2(Graph.apply)
}

