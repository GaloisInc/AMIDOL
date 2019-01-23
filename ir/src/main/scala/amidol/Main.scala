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
    var currentModel: Graph = Graph(Map.empty, Map.empty) 
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
        getFromFile("../ui/index.html")  // Serve up the Elm-generated UI
      } ~
      pathPrefix("appstate") {
        path("model") {
          complete(ui.convert.graphRepr.toUi(AppState.currentModel))
        }
      } ~
      pathPrefix("backends") {
        pathPrefix("scipy") {
          path("integrate") {
            parameters('inputs.as[SciPyIntegrate.Inputs]) { inputs =>
              complete(SciPyIntegrate.routeComplete(AppState.currentModel, inputs))
            }
          }
        }
      }
    } ~
    post {
      pathPrefix("appstate") {
        path("model") {
          entity(as[ui.Graph]) { newModel: ui.Graph =>
            complete {
              ui.convert.graphRepr.fromUi(newModel) match {
                case Success(parsed) =>
                  AppState.currentModel = parsed
                  StatusCodes.Created -> s"Model has been updated"

                case Failure(f) =>
                  StatusCodes.BadRequest -> s"Couldn't parse model: ${f.toString}"
              }
            }
          }
        }
      }
    }
  }

  // Start the server
  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
