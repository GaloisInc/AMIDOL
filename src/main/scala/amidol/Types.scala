package amidol

import amidol.math._

import java.util.Date

// All of this file is subject to a ton of change! No idea what the best way is to handle this...

case class StateId(id: String) extends AnyVal
case class EventId(id: String) extends AnyVal

case class Model(
  states: Map[StateId, State],
  events: Map[EventId, Event]
)

case class State(
  id: StateId,
  stateVariable: Variable,
  description: Option[String],
)

case class Event(
  id: EventId,
  rate: Expr[Double],
  input_predicate: Option[InputPredicate],
  output_predicate: Option[OutputPredicate],
  description: Option[String],
)

case class InputPredicate(
  enabling_condition: Expr[Boolean]
)
case class OutputPredicate(
  transition_function: Map[StateId, Expr[Double]]
)
