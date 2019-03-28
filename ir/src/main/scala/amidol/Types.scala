package amidol

import amidol.math._

import java.util.Date

// All of this file is subject to a ton of change! No idea what the best way is to handle this...

case class NounId(id: String) extends AnyVal
case class VerbId(id: String) extends AnyVal

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

case class Verb(
  id: VerbId,
  source: NounId, 
  target: NounId,
  label: Expr      // TODO rename to "rate" or such
)


