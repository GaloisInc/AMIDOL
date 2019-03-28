package amidol

import amidol.math._

import java.util.Date

// All of this file is subject to a ton of change! No idea what the best way is to handle this...

case class NounId(id: Long) extends AnyVal
case class VerbId(id: Long) extends AnyVal

case class Model(
  nouns: Map[NounId, Noun],
  verbs: Map[VerbId, Verb]
)

case class Noun(
  id: NounId,
  stateVariable: Variable,
  
  // Only for pruposes of recreating ui.Graph. TODO: should we remove?
  view: String,
  location: ui.Point
)

sealed trait Verb
case class Conserved(
  id: VerbId,
  source: NounId,
  target: NounId,
  rate: Expr
) extends Verb

case class Unconserved(
  id: VerbId,
  source: NounId, 
  target: NounId,
  rateOut: Expr,
  rateIn: Expr
) extends Verb

case class Source(
  id: VerbId,
  target: NounId,
  rateIn: Expr
) extends Verb

case class Sink(
  id: VerbId,
  source: NounId,
  rateOut: Expr
) extends Verb
