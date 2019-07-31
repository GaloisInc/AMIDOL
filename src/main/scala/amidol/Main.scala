package amidol

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.ActorMaterializer
import amidol.backends._
import scala.io.{Source, StdIn}
import scala.util._
import scala.collection.concurrent

import spray.json._

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap
import java.nio.file.Files
import java.nio.file.Paths

import com.typesafe.config.ConfigFactory

object Main extends App with Directives {

  val conf = ConfigFactory.load();

  // Mutable app state
  //
  // TODO: eventually, think about thread safety here (what happens if someone changes the model
  // while the backend is running?)
  object AppState {
    var currentModel: Model = Model.empty
    var palette: Map[String, Model] = List(
        "cure", "population", "infect", "patient",
        "population_vital_dynamics", "patient_vital_dynamics", "time",
        "predator", "prey", "hunting"
      )
      .map { name: String =>
        val modelSource = Source.fromResource(s"palette/$name.air").getLines.mkString("\n")
        name -> modelSource.parseJson.convertTo[Model]
      }
      .toMap

    val dataTraces: concurrent.Map[String, (Vector[Double], Vector[Double])] = {
      import scala.collection.JavaConverters._
      new ConcurrentHashMap().asScala
    }
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
        getFromResourceDirectory("web")
      } ~
      pathPrefix("appstate") {
        path("model") {
          complete(AppState.currentModel)
        }
      }
    } ~
    options {
      complete(
        Success("Yay")
      )
    } ~
    post  {
      pathPrefix("appstate") {
        formField('model.as[Model]) { case model: Model =>
          complete {
            AppState.currentModel = model
            StatusCodes.Created -> s"Model has been updated"
          }
        } ~
        path("uiModel") {
          formField('graph.as[ui.Graph]) { case graph: ui.Graph =>
            complete {
              graph.parse(AppState.palette).map { model =>
                AppState.currentModel = model
              } match {
                case Success(_) => StatusCodes.Created -> s"Model has been updated"
                case Failure(f) => StatusCodes.BadRequest -> f.getMessage
              }
            }
          }
        } ~
        pathPrefix("data-traces") {
          import SprayJsonSupport._
          import DefaultJsonProtocol._

          path("put") {
            formField('name.as[String], 'time.as[Vector[Double]], 'data.as[Vector[Double]]) {
              case (name: String, time: Vector[Double], trace: Vector[Double]) =>
                complete {
                  AppState.dataTraces += (name -> (time, trace))
                  StatusCodes.Created -> s"Data trace has been added"
                }
            }
          } ~
          path("remove") {
            formField('name.as[String]) { case name: String =>
              complete {
                AppState.dataTraces -= name
                StatusCodes.OK -> "Data trace has been removed"
              }
            }
          } ~
          path("get") {
            formField('names.as[List[String]]) { case names =>
              complete {
                StatusCodes.OK -> names
                  .map { name => (name, AppState.dataTraces.get(name)) }
                  .collect { case (name, Some((label, series))) => (name, label, series) }
              }
            }
          } ~
          path("list") {
            formField('limit.as[Long]) { case limit: Long =>
              complete {
                StatusCodes.OK -> AppState.dataTraces.keys.take(limit.toInt) // limit.fold(Int.MaxValue)(_.toInt))
              }
            }
          }

        } ~
        pathPrefix("palette") {
          path("put") {
            formField('name.as[String], 'model.as[Model]) { case (name: String, model: Model) =>
              complete {
                AppState.palette += (name -> model)
                StatusCodes.Created -> s"Palette has been updated"
              }
            }
          } ~
          path("remove") {
            formField('name.as[String]) { case name: String =>
              complete {
                AppState.palette -= name
                StatusCodes.OK -> s"Palette has been removed"
              }
            }
          } ~
          path("get") {
            formField('name.as[String]) { case name: String =>
              complete {
                StatusCodes.OK -> AppState.palette.get(name)
              }
            }
          }
        } ~
        path("julia") {
         // uploadedFile("txt") {
         //   case (metadata, file) =>
         //     println("file received " + file.length() );
         //     complete("Model received")
         // }
          formField(
            'julia.as[String]
          ) { case juliaSourceCode: String =>
           
            complete {
              JuliaSExpr(juliaSourceCode).flatMap(sexpr => Try(sexpr.extractModel())) match {
                case Success(model) =>
                  AppState.currentModel = model
                  StatusCodes.Created -> s"Model has been updated"
              
                case Failure(f) =>
                  StatusCodes.BadRequest -> f.getMessage
              }
            }
          }
        }
      } ~
      pathPrefix("backends") {
        pathPrefix("julia") {
          path("integrate") {
            import JuliaGillespie._

            println("Received request")
            entity(as[Inputs]) { inputs =>
              complete(
                StatusCodes.OK -> routeComplete(
                  AppState.currentModel,
                  inputs,
                  AppState.requestId.incrementAndGet()
                )
              )
            }
          }
        } ~
        pathPrefix("scipy") {
          path("integrate") {
            import SciPyIntegrate._
            
            entity(as[Inputs]) { inputs =>
              complete(
                StatusCodes.OK -> routeComplete(
                  AppState.currentModel,
                  inputs,
                  AppState.requestId.incrementAndGet()
                )
              )
            }
          } /* ~
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
          } */
        } /* ~
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
        } */
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
