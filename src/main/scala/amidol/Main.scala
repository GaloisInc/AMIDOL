package amidol

import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, Directives}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.webjars.WebJarAssetLocator
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

object Main extends App with Directives {

  val conf = ConfigFactory.load();
  OntologyDb // Initialize db

  // Mutable app state
  //
  // TODO: eventually, think about thread safety here (what happens if someone changes the model
  // while the backend is running?)
  class AppState() {
    var currentModel: Model = Model.empty
    var paletteItems: Map[String, PaletteItem] = getPaletteItems()

    // Load up palette elements from resources
    def getPaletteItems(): Map[String, PaletteItem] =
      List(
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

    val dataTraces: concurrent.Map[String, math.Trace] = {
      import scala.collection.JavaConverters._
      new ConcurrentHashMap().asScala
    }

    def reset(): Unit = {
      println("Resetting state...")
      currentModel = Model.empty
      paletteItems = getPaletteItems()
      dataTraces.clear()
    }
  }
  val requestId: AtomicLong = new AtomicLong()

  // Set up actor system and contexts
  implicit val system = ActorSystem("my-system")
  implicit val executionContext = system.dispatcher

  // Set up the folder for temporary files
  Files.createDirectories(Paths.get("tmp_scripts"))

  private val webJarAssetLocator = new WebJarAssetLocator()

  /** Get the AMIDOL_ID cookie, or create a new random cookie if one is not
   *  already defined. Then, use this cookie to choose the right appstate.
   *
   *  Every appstate automatically gets reset after an hour.
   */
  val cookiedAppState: Directive1[AppState] = {

    /** All of the current appstates, keyed under the cookie that reaches them */
    val states: concurrent.Map[String, AppState] = concurrent.TrieMap.empty[String, AppState]

    /** The tasks to clear the appstate map */
    val resets: concurrent.Map[String, Cancellable] = concurrent.TrieMap.empty[String, Cancellable]
    val timeTillReset: FiniteDuration =
      conf.getDuration("amidol.app-state-reset-time").toNanos().nanoseconds

    /** Reset the time until a certain appstate will be flushed out */
    def startResetTime(amidolId: String): Unit = {
      val resetTask = system.scheduler.scheduleOnce(timeTillReset) {
        println(s"Clearing out the app state for AMIDOL_ID=$amidolId")
        states.remove(amidolId)
        resets.remove(amidolId)
      }
      resets.put(amidolId, resetTask) match {
        case Some(previousResetTask) => previousResetTask.cancel()
        case None => /* nothing to do */
      }
    }

    optionalCookie("AMIDOL_ID").flatMap {
      case Some(existing) if states.contains(existing.value) =>
        startResetTime(existing.value)
        provide(states(existing.value))

      case _missing =>
        val newId: String = java.util.UUID.randomUUID().toString()
        val newState = new AppState()
        states += (newId -> newState)
        println(s"Created new cookie AMIDOL_ID=$newId")

        setCookie(HttpCookie("AMIDOL_ID", value = newId)).tflatMap { _ =>
          startResetTime(newId)
          provide(newState)
        }
    }
  }

  val route = cookiedAppState { appState: AppState =>

    val s = appState
    val localRoutes = new AkkaHttpRoutes {
      val appState = s
    }

    DocumentationServer.routes ~
    localRoutes.routes ~
    get {
      path("") {
        getFromResource("web/graph.html")
      } ~
      pathPrefix("") {
        extractUnmatchedPath {
          case path if path.toString.endsWith(".js.map") =>
            println(s"Triggered $path")
            /* See "Source map workaround" in `project/Webpack.scala` */
            getFromResource(s"web/${path.toString.stripSuffix(".js.map")}.js-map")

          case _ => reject
        }
      } ~
      pathPrefix("") {
        getFromResourceDirectory("web")
      } ~
      pathPrefix("") {  // Go get resources from WebJars
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
          complete(appState.currentModel)
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
            appState.currentModel = model
            StatusCodes.Created -> s"Model has been updated"
          }
        } ~
        path("reset") {
          complete {
            appState.reset()
            println("Clearing all application state")
            StatusCodes.Created -> s"State has been reset"
          }
        } ~
        path("uiModel") {
          formField('graph.as[ui.Graph]) { case graph: ui.Graph =>
            complete {
              graph.parse(appState.paletteItems).map { model =>
                appState.currentModel = model
              } match {
                case Success(_) => StatusCodes.Created -> s"Model has been updated"
                case Failure(f) => StatusCodes.BadRequest -> f.getMessage
              }
            }
          }
        } ~
        path("uiDiffEqs") {
          formField('equations.as[LatexEquations]) { case LatexEquations(equations) =>
            complete {
              LatexExtract.extractFromSource(equations).map { model =>
                appState.currentModel = model
              } match {
                case Success(_) => StatusCodes.Created -> s"Model has been updated"
                case Failure(f) => StatusCodes.BadRequest -> f.getMessage()
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
                  appState.paletteItems ++= newPalElems
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
                  appState.dataTraces += (name -> math.SampledTrace(time, trace))
                  StatusCodes.Created -> s"Data trace has been added"
                }
            }
          } ~
          path("remove") {
            formField('name.as[String]) { case name: String =>
              complete {
                appState.dataTraces -= name
                StatusCodes.OK -> "Data trace has been removed"
              }
            }
          } ~
          path("get") {
            formField('names.as[List[String]]) { case names =>
              complete {
                StatusCodes.OK -> names
                  .map { name => (name, appState.dataTraces.get(name)) }
                  .collect { case (name, Some(math.SampledTrace(label, series))) => (name, label, series) }
              }
            }
          } ~
          path("list") {
            formField('limit.as[Long]) { case limit: Long =>
              complete {
                StatusCodes.OK -> appState.dataTraces.keys.take(limit.toInt) // limit.fold(Int.MaxValue)(_.toInt))
              }
            }
          } ~
          path("eval") {
            formField("query".as[String]) { case query: String =>
              complete {
                math.Trace(query, appState.dataTraces.toMap)
                  .map {
                    case s: math.SampledTrace => s
                    case p: math.PureFunc => p.sampleAt(collection.immutable.Range(0, 100, 1).toVector.map(_.toDouble))
                  }
                  .map {
                    case math.SampledTrace(xs,ys) => (xs,ys)
                  }
                  .fold(
                    err => StatusCodes.BadRequest -> err.getMessage,
                    success => StatusCodes.OK -> success
                  )
              }
            }
          }

        } ~
        pathPrefix("palette") {
          path("put") {
            formField('name.as[String], 'palette.as[PaletteItem]) { case (name: String, p: PaletteItem) =>
              complete {
                appState.paletteItems += (name -> p)
                StatusCodes.Created -> s"Palette has been updated"
              }
            }
          } ~
          path("remove") {
            formField('name.as[String]) { case name: String =>
              complete {
                appState.paletteItems -= name
                StatusCodes.OK -> s"Palette has been removed"
              }
            }
          } ~
          path("get") {
            formField('name.as[String]) { case name: String =>
              complete {
                StatusCodes.OK -> appState.paletteItems.get(name)
              }
            }
          } ~
          path("list") {
            formField('limit.as[Int].?) { case limit: Option[Int] =>
              complete {
                StatusCodes.OK -> {
                  val values = appState.paletteItems.values
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
                  appState.currentModel = model
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
                  appState.currentModel,
                  appState,
                  inputs,
                  requestId.incrementAndGet()
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
                  appState.currentModel,
                  appState,
                  inputs,
                  requestId.incrementAndGet()
                )
              )
            }
          } /* ~
          path("cmtc-equilibrium") {
            entity(as[SciPyLinearSteadyState.Inputs]) { inputs =>
              complete(
                StatusCodes.OK -> SciPyLinearSteadyState.routeComplete(
                  appState.currentModel,
                  appState.currentGlobalConstants,
                  appState.currentInitialConditions,
                  inputs,
                  requestId.incrementAndGet()
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
                  appState.currentModel,
                  appState.currentGlobalConstants,
                  appState.currentInitialConditions,
                  inputs,
                  requestId.incrementAndGet()
                )
              )
            }
          }
        } */
      }
    }
  }
  val routeAcao = toStrictEntity(3.seconds) {
    respondWithHeader(`Access-Control-Allow-Origin`(HttpOriginRange.*)) {
      route
    }
  }


  // Start the server
  val address = conf.getString("amidol.address") // "52.43.67.227"
  val port = conf.getInt("amidol.port") // 80
  val bindingFuture = Http().bindAndHandle(routeAcao, address, port)

  println(s"Server online at $address:$port/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
