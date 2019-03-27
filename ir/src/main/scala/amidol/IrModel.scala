package amidol

import amidol.math._

case class IrModel(
  name: Symbol,
  stateVariables: List[StateVariable], 
  events: List[Event],
  constants: List[Constant],
  expressions: List[AliasedExpression],
  rateRewards: List[RateReward],
  impulseRewards: List[Nothing], // no examples yet
  composedRewards: List[Nothing] // no examples yet
) {

  def compose(other: IrModel): IrModel = IrModel(
    name = Symbol(this.name.name + "c" + other.name.name),
    stateVariables = this.stateVariables ++ other.stateVariables,
    events = this.events ++ other.events,
    constants = this.constants ++ other.constants,
    expressions = this.expressions ++ other.expressions,
    rateRewards = this.rateRewards ++ other.rateRewards,
    impulseRewards = this.impulseRewards ++ other.impulseRewards,
    composedRewards = this.composedRewards ++ other.composedRewards
  )

}

case class StateVariable(
  name: Symbol,
  label: String,
 // `type`: Float|Int|...
  initial_value: Float // or is it an expression...?
)

case class Event(
  name: Symbol,
  label: String,
  rate: Expr,
  input_predicate: Option[Nothing], // no examples yet
  output_predicate: Option[Nothing] // no examples yet
)

case class Constant(
  name: Symbol,
  value: Expr
)

case class AliasedExpression(
  name: Symbol,
  value: Expr
)

case class RateReward(
  name: Symbol,
  variable: Symbol,
  temporalType: TemporalType,
  samplingPoints: List[Long]  // times at which to sample
)

sealed trait TemporalType
case object InstantOfTime extends TemporalType
