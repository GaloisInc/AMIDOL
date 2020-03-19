package amidol

import scala.util._
import spray.json._
import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport


package object backends {

  trait Backend {

    /** What is the problem solved by this backend? */
    val problemDescription: String

    /** What does this backend do (from a technical perspective)? */
    val backendDescription: String

    /** Descriptive name for this backend */
    val name: String

    /** Extra inputs to the backend (on top of the model loaded in) */
    type Inputs
    implicit def inputsFormat: RootJsonFormat[Inputs]

    /** Outputs from the backend */
    type Outputs
    implicit def outputsFormat: RootJsonFormat[Outputs]

    /** Is this a backend we can [[run]] on our [[Model]]? */
    def applicable(model: Model): Boolean

    /** Run the backend */
    def run(
      model: Model,
      appState: Main.AppState,
      inputs: Inputs,
      requestId: Long
    )(implicit
      ec: ExecutionContext
    ): Future[Try[Outputs]]

    final def routeComplete(
      model: Model,
      appState: Main.AppState,
      inputs: Inputs,
      requestId: Long
    )(implicit
      ec: ExecutionContext
    ): Future[Option[Outputs]] =
      Future(applicable(model)).flatMap { isApplicable: Boolean =>
        if (isApplicable) {
          run(model, appState, inputs, requestId).map {
            case Success(outputs) => Some(outputs)
            case Failure(err) =>
              println(s"Failed to run ${name} backend: ${err.getMessage}")
              None
          }
        } else {
          println(s"Backend ${name} is not applicable to model")
          Future.successful(None)
        }
      }
  }


  trait ContinuousInitialValue extends Backend with SprayJsonSupport with DefaultJsonProtocol with NullOptions {
    val problemDescription = "Continuous initial value problem"

    case class Inputs(
      initialTime: Double,
      finalTime: Double,
      stepSize: Double,
      savePlot: Option[String] // saves an image version of the plot at this path
    )

    case class Outputs(
      variables: Map[String, Seq[Double]],
      times: Seq[Double]
    )

    implicit val inputsFormat: RootJsonFormat[Inputs] = jsonFormat4(Inputs.apply)
    implicit val outputsFormat: RootJsonFormat[Outputs] = jsonFormat2(Outputs.apply)
  }

  trait ContinuousSteadyState extends Backend with SprayJsonSupport with DefaultJsonProtocol {
    val problemDescription = "Find equilibrium points in a model"

    case class Inputs()

    case class Equilibrium(
      stable: Option[Boolean]
    )
    case class Outputs(
      equilibriums: Seq[Equilibrium]
    )

    implicit val inputsFormat: RootJsonFormat[Inputs] = jsonFormat0(() => Inputs.apply())
    implicit val equilibriumFormat: RootJsonFormat[Equilibrium] = jsonFormat1(Equilibrium.apply)
    implicit val outputsFormat: RootJsonFormat[Outputs] = jsonFormat1(Outputs.apply)
  }
}
