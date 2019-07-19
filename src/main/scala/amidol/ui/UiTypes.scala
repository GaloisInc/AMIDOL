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

    val models = Map.newBuilder[String, Model]
    val shared = List.newBuilder[((String, StateId), (String, StateId))] 
    val variableRename = Map.newBuilder[String, String]

    for (Node(id, image, label, props, x, y) <- nodes.values) {
      val paletteModel = paletteModels.getOrElse(
        props.className,
        throw new Exception(s"No model called '${props.className}' found in palette")
      )

      // When we have a noun, try to give the state variable a good name
      if (props.`type` == "noun" && props.inState == props.outState) {
        variableRename += s"n${id}_${props.inState}" -> label
      }
 
      val constants = props.parameters
        .map { case Parameter(n,v) => math.Variable(Symbol(n)) -> v.asConstant.get.d }
        .toMap

      models += ("n" + id) -> paletteModel.copy(constants = paletteModel.constants ++ constants)
     
      if (props.`type` == "verb") {
        
        val nounFrom = getIncoming(id)
        shared += ((("n" + id, amidol.StateId(props.inState)), ("n" + nounFrom, amidol.StateId(nodes(nounFrom).props.outState))))
        
        val nounTo = getOutgoing(id)
        shared += ((("n" + id, amidol.StateId(props.outState)), ("n" + nounTo, amidol.StateId(nodes(nounTo).props.inState))))
      }
    }

    val rename = variableRename.result().map { case (k,v) => math.Variable(Symbol(k)) -> math.Variable(Symbol(v)) }

    println("To compose: " + models.result())
    println("To share: " + shared.result())
    val out = amidol.Model.composeModels(models.result(), shared.result())
    println("Out: " + out)
    out.mapIds(identity, identity, v => rename.getOrElse(v,v))
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
  inState: String,  // stateid
  outState: String, // stateid
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
  implicit val nodePropsFormat = jsonFormat6(NodeProps.apply)
  implicit val linkFormat = jsonFormat3(Link.apply)
  implicit val nodeFormat = jsonFormat6(Node.apply)
  implicit val graphFormat = jsonFormat2(Graph.apply)
}

