package amidol

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import amidol.backends._

import scala.io.StdIn
import scala.util._

import spray.json._


object Main extends App with Directives with ui.UiJsonSupport {

  // Mutable app state
  //
  // TODO: eventually, think about thread safety here (what happens if someone changes the model
  // while the backend is running?)
  object AppState {
    var currentModel: Model = Model(Map.empty, Map.empty)
    var currentGlobalConstants: Map[String, Double] = Map.empty   // TODO: these should be validated _before_ beingn written in
    var currentInitialConditions: Map[String, Double] = Map.empty // TODO: these should be validated _before_ beingn written in
  }

  // Set up actor system and contexts
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  // How to route requests.
  //
  //   * curl -H "Content-Type: application/json" -X POST -d @src/main/resources/sirs_graph.json "http://localhost:8080/appstate/model"
  //   * curl "http://localhost:8080/appstate/model"
  //
  val route = respondWithHeader(`Access-Control-Allow-Origin`(HttpOriginRange.*)) {
    get {
      path("") {
        getFromResource("web/graph.html")
      } ~
      pathPrefix("") {
        getFromDirectory(new java.io.File("src/main/resources/web").getCanonicalPath)
      } ~
      pathPrefix("appstate") {
        path("model") {
          complete(ui.convert.graphRepr.toUi(AppState.currentModel))
        }
      }
    } ~
    options {
      complete(
        Success("Yay")
      )
    } ~
    post {
      path("appstate") {
        formField(
          'graph.as[uinew.Graph],
          'globalVariables.as[Map[String,Double]]
        ) { case (graph: uinew.Graph, globalVariables: Map[String,Double]) =>
          complete {
            uinew.convert.graphRepr.fromUi(graph) match {
              case Success((parsed, initialConds)) =>
                AppState.currentModel = parsed
                AppState.currentGlobalConstants = globalVariables
                AppState.currentInitialConditions = initialConds
                StatusCodes.Created -> s"Model has been updated"

              case Failure(f) =>
                StatusCodes.BadRequest -> s"Couldn't parse model: ${f.toString}"
            }
          }
        }
      } ~
      pathPrefix("backends") {
        pathPrefix("scipy") {
          path("integrate") {
            entity(as[SciPyIntegrate.Inputs]) { inputs =>
              complete(
                StatusCodes.OK -> SciPyIntegrate.routeComplete(
                  AppState.currentModel,
                  AppState.currentGlobalConstants,
                  AppState.currentInitialConditions,
                  inputs
                )
              )
            }
          } ~
          path("cmtc-equilibrium") {
            entity(as[SciPyLinearSteadyState.Inputs]) { inputs =>
              complete(
                StatusCodes.OK -> SciPyLinearSteadyState.routeComplete(
                  AppState.currentModel,
                  AppState.currentGlobalConstants,
                  AppState.currentInitialConditions,
                  inputs
                )
              )
            }
          }
        } ~
        pathPrefix("pysces") {
          path("integrate") {
            entity(as[PySCeS.Inputs]) { inputs =>
              complete(
                StatusCodes.OK -> PySCeS.routeComplete(
                  AppState.currentModel,
                  AppState.currentGlobalConstants,
                  AppState.currentInitialConditions,
                  inputs
                )
              )
            }
          }
        }
      }
    }
  }

  // Start the server
  val address = "localhost"
  val port = 8080
  val bindingFuture = Http().bindAndHandle(route, address, port)

  println(s"Server online at http://$address:$port/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
