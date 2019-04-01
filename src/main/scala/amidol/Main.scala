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

import java.util.concurrent.atomic.AtomicLong
import java.nio.file.Files
import java.nio.file.Paths

import com.typesafe.config.ConfigFactory

object Main extends App with Directives with ui.UiJsonSupport {

  val conf = ConfigFactory.load();

  // Mutable app state
  //
  // TODO: eventually, think about thread safety here (what happens if someone changes the model
  // while the backend is running?)
  object AppState {
    var currentUiGraph: ui.Graph = ui.Graph(Map.empty, Map.empty)

    var currentModel: Model = Model(Map.empty, Map.empty)
    var currentGlobalConstants: Map[String, Double] = Map.empty   // TODO: these should be validated _before_ being written in
    var currentInitialConditions: Map[String, Double] = Map.empty // TODO: these should be validated _before_ being written in

    val requestId: AtomicLong = new AtomicLong()
  }

  // Set up actor system and contexts
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher


  // Set up the folder for temporary files
  Files.createDirectories(Paths.get("tmp_scripts"))

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
          complete(AppState.currentUiGraph)
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
          'graph.as[ui.Graph],
          'globalVariables.as[Map[String,Double]]
        ) { case (graph: ui.Graph, globalVariables: Map[String,Double]) =>
          complete {
            graph.parse() match {
              case Success((parsed, initialConds)) =>
                AppState.currentModel = parsed
                AppState.currentGlobalConstants = globalVariables
                AppState.currentInitialConditions = initialConds
                AppState.currentUiGraph = graph
                StatusCodes.Created -> s"Model has been updated"

              case Failure(f) =>
                StatusCodes.BadRequest -> f.getMessage
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
                  inputs,
                  AppState.requestId.incrementAndGet()
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
                  inputs,
                  AppState.requestId.incrementAndGet()
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
                  inputs,
                  AppState.requestId.incrementAndGet()
                )
              )
            }
          }
        }
      }
    }
  }

  // Start the server
  val address = conf.getString("amidol.address") // "52.43.67.227"
  val port = conf.getInt("amidol.port") // 80
  val bindingFuture = Http().bindAndHandle(route, address, port)

  println(s"Server online at $address:$port/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
