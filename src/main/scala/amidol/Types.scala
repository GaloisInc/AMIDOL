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
) {

  def ++(other: Model) = Model(
    states ++ other.states,
    events ++ other.events,
    constants ++ other.constants,
  )

  def mapIds(
    stateIdFunc: StateId => StateId,
    eventIdFunc: EventId => EventId,
    variableFunc: Variable => Variable,
  ): Model = {
    val newStates = states
      .map { case (sid, State(sv, desc, init)) =>
        val newSid = stateIdFunc(sid)
        val newSv = variableFunc(sv)
        val newInit = init.mapVariables(variableFunc)
        newSid -> State(newSv, desc, newInit)
      }

    val newEvents = events
      .map { case (eid, Event(rate, ipOpt, OutputPredicate(tf), desc)) =>
        val newEid = eventIdFunc(eid)
        val newRate = rate.mapVariables(variableFunc)
        val newIp = ipOpt.map { case InputPredicate(ec) =>
          InputPredicate(ec.mapVariables(variableFunc))
        }
        val newTf = tf.map { case (sid, eff) =>
          val newSid = stateIdFunc(sid)
          val newEff = eff.mapVariables(variableFunc)
          newSid -> newEff
        }
        newEid -> Event(newRate, newIp, OutputPredicate(newTf), desc)
      }

    val newConstants = constants
      .map { case (v, d) =>
        variableFunc(v) -> d
      }

    Model(newStates, newEvents, newConstants)
  }
}
object Model extends ModelJsonSupport {

  val empty = Model(Map.empty, Map.empty, Map.empty)

  def composeModels[K: Ordering](
    models: List[(K, Model)],                      // models to compose
    shared: List[((K, StateId), (K, StateId))], // states shared amongst submodels
  ): Model = {
    val sharedMap = shared.toMap

    def stateIdFunc(k: K)(stateId: StateId): StateId = {
      sharedMap.get(k -> stateId) match {
        case None => StateId(s"${k}_${stateId.id}")
        case Some((k2, stateId2)) => StateId(s"${k2}_${stateId2.id}")
      }
    }
    def eventIdFunc(k: K)(eventId: EventId): EventId = EventId(s"${k}_${eventId.id}")
    def variableFunc(k: K)(v: Variable): Variable = {
      sharedMap.get(k -> StateId(v.s.name)) match  {
        case None => Variable(Symbol(s"${k}_${v.s.name}"))
        case Some((k2, stateId2)) => Variable(Symbol(s"${k2}_${stateId2.id}"))
      }
    }

    models
      .iterator
      .map { case (k,m) => m.mapIds(stateIdFunc(k), eventIdFunc(k), variableFunc(k)) }
      .foldLeft(Model.empty)(_ ++ _)
  }
}

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
  implicit val paletteFormat = jsonFormat6(PaletteItem.apply)
}

// Full description of a palette element
case class PaletteItem(
  className: String,
  `type`: String,
  sharedStates: Array[StateId],        // For now, this is be: `[ <shared state for incoming arrows>
                                       //                       , <shared state for outgoing arrows> ]
  icon: String,
  color: Option[String],
  backingModel: amidol.Model,   // parameters = backingModel.constants
)
object PaletteItem extends ModelJsonSupport

