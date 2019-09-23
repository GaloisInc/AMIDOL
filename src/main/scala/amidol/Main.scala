package amidol

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.webjars.WebJarAssetLocator
import akka.stream.ActorMaterializer
import amidol.backends._
import scala.io.{Source, StdIn}
import scala.util._
import scala.collection.concurrent
import scala.concurrent.duration._

import spray.json._

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap
import java.nio.file.Files
import java.nio.file.Paths

import com.typesafe.config.ConfigFactory
import java.io.File

object Main extends App with Directives { app =>

  val conf = ConfigFactory.load();
  OntologyDb // Initialize db

  // Mutable app state
  //
  // TODO: eventually, think about thread safety here (what happens if someone changes the model
  // while the backend is running?)
  class AppState() {
    var currentModel: Model = Model.empty
    var paletteItems: Map[String, PaletteItem] = List(
        "cure", "population", "infect", "patient",
        "population_vital_dynamics", "patient_vital_dynamics", "time",
        "predator", "prey", "hunting"
      )
      .map { name: String =>
        val modelSource = Source.fromResource(s"palette/$name.air").getLines.mkString("\n")
        name -> Try(modelSource.parseJson.convertTo[PaletteItem]).recoverWith {
          case err =>
            println(s"Failure in $name")
            Failure(err)
        }.get
      }
      .toMap

    val dataTraces: concurrent.Map[String, (Vector[Double], Vector[Double])] = {
      import scala.collection.JavaConverters._
      new ConcurrentHashMap().asScala
    }
    val requestId: AtomicLong = new AtomicLong()
  }
  var state = new AppState()

  // Set up actor system and contexts
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  // Set up the folder for temporary files
  Files.createDirectories(Paths.get("tmp_scripts"))

  private val webJarAssetLocator = new WebJarAssetLocator()
  val route = respondWithHeader(`Access-Control-Allow-Origin`(HttpOriginRange.*)) {
    get {
      path("") {
        getFromResource("web/graph.html")
      } ~
      pathPrefix("") {
        getFromResourceDirectory("web")
      } ~
      pathPrefix("lib") {  // Go get resources from WebJars
        extractUnmatchedPath { path =>
          Try(webJarAssetLocator.getFullPath(path.toString)) match {
            case Success(fullPath) => getFromResource(fullPath)
            case Failure(_: IllegalArgumentException) => reject
            case Failure(err) => failWith(err)
          }
        }
      } ~
      pathPrefix("appstate") {
        path("model") {
          complete(app.state.currentModel)
        }
      } ~
      pathPrefix("ontology") {
        path("search") {
          parameters('term.as[String], 'limit.as[Long].?, 'seconds.as[Long].?) {
            case (term: String, limitOpt: Option[Long], secondsOpt: Option[Long]) =>
              val tempSvg = File.createTempFile("search", ".svg")
              tempSvg.deleteOnExit()
              OntologyDb.searchForFirst(
                searchTerm = term,
                searchMatch = _.annotations.nonEmpty,
                haltOnMatch = false,
                limit = limitOpt.getOrElse(Long.MaxValue),
                deadline = secondsOpt.fold(1.day)(_.seconds).fromNow,
                createSearchDotImage = Some(tempSvg.getAbsolutePath()),
              )
              getFromFile(tempSvg)
          }
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
            app.state.currentModel = model
            StatusCodes.Created -> s"Model has been updated"
          }
        } ~
        path("reset") {
          complete {
            app.state = new AppState()
            println("Clearing all application state")
            StatusCodes.Created -> s"State has been reset"
          }
        } ~
        path("uiModel") {
          formField('graph.as[ui.Graph]) { case graph: ui.Graph =>
            complete {
              graph.parse(app.state.paletteItems).map { model =>
                app.state.currentModel = model
              } match {
                case Success(_) => StatusCodes.Created -> s"Model has been updated"
                case Failure(f) => StatusCodes.BadRequest -> f.getMessage
              }
            }
          }
        } ~
        path("loadJuliaModel") {
          import SprayJsonSupport._
          import DefaultJsonProtocol._

          formFields('juliaSourceCode, 'name) { case (juliaSrc, name) =>
            complete {
              JuliaExtract.extractFromSource(juliaSrc, name) match {
                case Failure(f) => StatusCodes.BadRequest -> f.getMessage()
                case Success((uiGraph, newPalElems)) =>
                  app.state.paletteItems ++= newPalElems
                  StatusCodes.Created -> uiGraph
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
                  app.state.dataTraces += (name -> (time, trace))
                  StatusCodes.Created -> s"Data trace has been added"
                }
            }
          } ~
          path("remove") {
            formField('name.as[String]) { case name: String =>
              complete {
                app.state.dataTraces -= name
                StatusCodes.OK -> "Data trace has been removed"
              }
            }
          } ~
          path("get") {
            formField('names.as[List[String]]) { case names =>
              complete {
                StatusCodes.OK -> names
                  .map { name => (name, app.state.dataTraces.get(name)) }
                  .collect { case (name, Some((label, series))) => (name, label, series) }
              }
            }
          } ~
          path("list") {
            formField('limit.as[Long]) { case limit: Long =>
              complete {
                StatusCodes.OK -> app.state.dataTraces.keys.take(limit.toInt) // limit.fold(Int.MaxValue)(_.toInt))
              }
            }
          }

        } ~
        pathPrefix("palette") {
          path("put") {
            formField('name.as[String], 'palette.as[PaletteItem]) { case (name: String, p: PaletteItem) =>
              complete {
                app.state.paletteItems += (name -> p)
                StatusCodes.Created -> s"Palette has been updated"
              }
            }
          } ~
          path("remove") {
            formField('name.as[String]) { case name: String =>
              complete {
                app.state.paletteItems -= name
                StatusCodes.OK -> s"Palette has been removed"
              }
            }
          } ~
          path("get") {
            formField('name.as[String]) { case name: String =>
              complete {
                StatusCodes.OK -> app.state.paletteItems.get(name)
              }
            }
          } ~
          path("list") {
            formField('limit.as[Int].?) { case limit: Option[Int] =>
              complete {
                StatusCodes.OK -> {
                  val values = app.state.paletteItems.values
                  limit.fold(values)(values.take(_))
                }
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
                  app.state.currentModel = model
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
                  app.state.currentModel,
                  inputs,
                  app.state.requestId.incrementAndGet()
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
                  app.state.currentModel,
                  inputs,
                  app.state.requestId.incrementAndGet()
                )
              )
            }
          } /* ~
          path("cmtc-equilibrium") {
            entity(as[SciPyLinearSteadyState.Inputs]) { inputs =>
              complete(
                StatusCodes.OK -> SciPyLinearSteadyState.routeComplete(
                  app.state.currentModel,
                  app.state.currentGlobalConstants,
                  app.state.currentInitialConditions,
                  inputs,
                  app.state.requestId.incrementAndGet()
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
                  app.state.currentModel,
                  app.state.currentGlobalConstants,
                  app.state.currentInitialConditions,
                  inputs,
                  app.state.requestId.incrementAndGet()
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
