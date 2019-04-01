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
)

sealed trait Event
case class Conserved(
  id: EventId,
  source: StateId,
  target: StateId,
  rate: Expr
) extends Event

case class Unconserved(
  id: EventId,
  source: StateId,
  target: StateId,
  rateOut: Expr,
  rateIn: Expr
) extends Event

case class Source(
  id: EventId,
  target: StateId,
  rateIn: Expr
) extends Event

case class Sink(
  id: EventId,
  source: StateId,
  rateOut: Expr
) extends Event
