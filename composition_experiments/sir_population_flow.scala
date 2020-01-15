
import spray.json._
import amidol._
import amidol.backends._

val sirModel = io.Source.fromFile("SIR-model.json"       ).mkString.parseJson.convertTo[Model]
val popModel = io.Source.fromFile("population-model.json").mkString.parseJson.convertTo[Model]


 val composed = Model.composeModels(
  models = List(
    "Pop_S" -> popModel,
    "Pop_I" -> popModel,
    "Pop_R" -> popModel,
    "Portland_SIR" -> sirModel,
    "Salem_SIR" -> sirModel,

  ),
  shared = List(
    ("Portland_SIR" -> StateId("susceptible"), "Pop_S" -> StateId("portland")),
    ("Salem_SIR"    -> StateId("susceptible"), "Pop_S" -> StateId("salem")),
    ("Portland_SIR" -> StateId("infected"),    "Pop_I" -> StateId("portland")),
    ("Salem_SIR"    -> StateId("infected"),    "Pop_I" -> StateId("salem")),
    ("Portland_SIR" -> StateId("recovered"),   "Pop_R" -> StateId("portland")),
    ("Salem_SIR"    -> StateId("recovered"),   "Pop_R" -> StateId("salem")),
  )
)

implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global


SciPyIntegrate.run(
  composed,
  appState = new Main.AppState(),
  inputs = SciPyIntegrate.Inputs(
    initialTime = 0.0,
    finalTime = 100.0,
    stepSize = 1.0,
    savePlot = Some("population_flow_with_SIR.png")
  ),
  requestId = 0L
)
