package amidol

import amidol.math._

import scala.util.{Failure, Success, Try}
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

case class StateId(id: String) extends AnyVal
case class EventId(id: String) extends AnyVal

case class Model(
  states: Map[StateId, State],
  events: Map[EventId, Event],
  constants: Map[Variable, Double],
)
object Model extends ModelJsonSupport

case class State(
  state_variable: Variable,
  description: Option[String],
  initial_value: Expr[Double], 
)

case class Event(
  rate: Expr[Double], // always exponential for now
  input_predicate: Option[InputPredicate] = None,
  output_predicate: OutputPredicate = OutputPredicate.empty,
  description: Option[String] = None,
)

case class InputPredicate(
  enabling_condition: Expr[Boolean]
)
case class OutputPredicate(
  transition_function: Map[StateId, Expr[Double]]
)
object OutputPredicate {
  def empty = OutputPredicate(Map.empty)
}


/* How should we render into JSON models?
 *
 * The new `StringLike` typeclass plays two roles in producing pretty JSON:
 *
 *    - string-like things are serialized as JSON strings
 *    - maps with string-like keys are turned straight into JSON objects (as opposed to an array
 *      of key-value tuples)
 */
trait ModelJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  trait StringLike[A] {
    def intoString(a: A): String
    def fromString(s: String): A
  }

  implicit object stringLikeEventId extends StringLike[EventId] {
    def intoString(e: EventId) = e.id
    def fromString(s: String) = EventId(s)
  }
  implicit object stringLikeStateId extends StringLike[StateId] {
    def intoString(s: StateId) = s.id
    def fromString(s: String) = StateId(s)
  }
  implicit object stringLikeVariable extends StringLike[Variable] {
    def intoString(v: Variable) = v.s.name
    def fromString(s: String) = Variable(Symbol(s))
  }

  /// Better format for maps with string-like keys
  implicit def objectFormat[K, A](implicit
    format: JsonFormat[A],
    key: StringLike[K],
  ) = new RootJsonFormat[Map[K, A]] {
    def read(json: JsValue) = json.asJsObject.fields.map {
      case (k,v) => key.fromString(k) -> format.read(v)
    }

    def write(map: Map[K, A]) = JsObject(map.map {
      case (k,v) => key.intoString(k) -> format.write(v)
    })
  }

  /// Derive a format from one type via the string format
  implicit def formatStringLike[A](implicit
    str: StringLike[A]
  ): JsonFormat[A] = new JsonFormat[A] {
    def write(a: A) = JsString(str.intoString(a))
    def read(json: JsValue) = json match {
      case JsString(s) => str.fromString(s)
      case _ => throw DeserializationException("Invalid string-like format: " + json)
    }
  }

  implicit val stateIdFormat = formatStringLike[StateId]
  implicit val eventIdFormat = formatStringLike[EventId]
  implicit val variableFormat = formatStringLike[Variable]

  implicit val outputPredFormat = jsonFormat1(OutputPredicate.apply)
  implicit val inputPredFormat = jsonFormat1(InputPredicate.apply)

  implicit val stateFormat = jsonFormat3(State.apply)
  implicit val eventFormat = jsonFormat4(Event.apply)
  implicit val modelFormat = jsonFormat3(Model.apply)
}
