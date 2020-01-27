
import spray.json._
import amidol._
import amidol.backends._
import amidol.math.Variable

def getModel(jsonFile: String) =
  io.Source.fromFile(jsonFile).mkString.parseJson.convertTo[Model]

val vdModel = getModel("composition_experiments/VD-model.json")
val sivrModel = getModel("composition_experiments/SIVRS-model.json")
val predModel = getModel("composition_experiments/predation-model.json")


// Infection cycles
val humanInfectionCycle = sivrModel
val mosquitoInfectionCycle = sivrModel.copy(
  constants = sivrModel.constants +
    (Variable('gamma) -> 0.0) + // mosquitos don't recover
    (Variable('mu) -> 0.0) +    // mosquitos do not re-become susceptible
    (Variable('beta) -> 0.5)    // mosquitos infect fast
)

// Vital dynamics
val healthyHumanVD = vdModel
val infectedHumanVD = vdModel.copy(
  constants = vdModel.constants +
    (Variable('death_rate) -> 0.030) // infected humans die 10x faster
)

val healthyMosquitoVD = vdModel.copy(
  constants = vdModel.constants +
    (Variable('birth_rate) -> 1) // mosquitos breed quickly
)
val infectedMosquitoVD = vdModel.copy(
  constants = vdModel.constants +
    (Variable('birth_rate) -> 0.5) // infected mosquitos breed less quickly
)

val birdVD = vdModel.copy(
  constants = vdModel.constants +
    (Variable('death_rate) -> 0.03) // birds die out faster due to competition with each other
)

// Preying
val healthyMosquitoPreying = predModel.copy(
  constants = predModel.constants +
    (Variable('conversion_ratio) -> 0.2)
)
val infectedMosquitoPreying = predModel.copy(
  constants = predModel.constants +
    (Variable('conversion_ratio) -> 0.1) // infected mosquitos provide less nutritional content
)


val composed = Model.composeModels(
  models = List(
    // Infection cycles
    "humanInfectionCycle" -> humanInfectionCycle,
    "mosquitoInfectionCycle" -> mosquitoInfectionCycle,

    // Human vital dynamics
    "susceptibleHumanVD" -> healthyHumanVD,
    "infectedHumanVD" -> infectedHumanVD,
    "recoveredHumanVD" -> healthyHumanVD,

    // Mosquito vital dynamics
    "susceptibleMosquitoVD" -> healthyMosquitoVD,
    "infectedMosquitoVD" -> infectedMosquitoVD,

    // Bird vital dynamics
    "birdVD" -> birdVD,

    // Predation
    "healthyPredation" -> healthyMosquitoPreying,
    "infectedPredation" -> infectedMosquitoPreying,
  ),
  shared = List(
    // Link the infection cycles together
    ("humanInfectionCycle"    -> StateId("virus"),       "mosquitoInfectionCycle" -> StateId("infected")),
    ("mosquitoInfectionCycle" -> StateId("virus"),       "humanInfectionCycle"    -> StateId("infected")),

    // Human  & Mosquito VD
    ("humanInfectionCycle"    -> StateId("susceptible"), "susceptibleHumanVD"    -> StateId("alive")),
    ("humanInfectionCycle"    -> StateId("infected"),    "infectedHumanVD"       -> StateId("alive")),
    ("humanInfectionCycle"    -> StateId("recovered"),   "recoveredHumanVD"      -> StateId("alive")),
    ("mosquitoInfectionCycle" -> StateId("susceptible"), "susceptibleMosquitoVD" -> StateId("alive")),
    ("mosquitoInfectionCycle" -> StateId("infected"),    "infectedMosquitoVD"    -> StateId("alive")),

    // Predation
    ("mosquitoInfectionCycle" -> StateId("susceptible"), "healthyPredation"      -> StateId("prey")),
    ("mosquitoInfectionCycle" -> StateId("infected"),    "infectedPredation"     -> StateId("prey")),
    ("birdVD"                 -> StateId("alive"),       "healthyPredation"      -> StateId("predator")),
    ("birdVD"                 -> StateId("alive"),       "infectedPredation"     -> StateId("predator")),
  )
)



implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

val initialConditions = Map(
  'virus_from_humanInfectionCycle          -> 10.0,  // aka infected mosquitos
  'susceptible_from_mosquitoInfectionCycle -> 100.0,
  'recovered_from_mosquitoInfectionCycle   -> 0.0,

  'virus_from_mosquitoInfectionCycle       -> 10.0,   // aka infected humans
  'susceptible_from_humanInfectionCycle    -> 100.0,
  'recovered_from_humanInfectionCycle      -> 0.0,

  'alive_from_birdVD                       -> 20.0   // aka bird population
)
val newStates = composed.states
  .mapValues { state: State =>
    val newInitialValue = math.Literal(initialConditions(state.state_variable.s))
    state.copy(initial_value = newInitialValue)
  }
  .toMap

SciPyIntegrate.run(
  composed.copy(states = newStates),
  appState = new Main.AppState(),
  inputs = SciPyIntegrate.Inputs(
    initialTime = 0.0,
    finalTime = 5.0,
    stepSize = 0.05,
    savePlot = Some("SIRS_with_predator_prey.png")
  ),
  requestId = 0L
)

