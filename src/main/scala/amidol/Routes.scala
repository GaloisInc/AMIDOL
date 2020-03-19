package amidol

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

import java.util.Base64
import java.util.concurrent.atomic.AtomicLong

import akka.util.{ByteString, Timeout}

import endpoints.{Valid, Invalid, algebra, generic, openapi}
import endpoints.generic.{name, docs}
import endpoints.akkahttp.server

import amidol.Main.AppState
import amidol.backends._

trait Routes
  extends algebra.Endpoints
    with algebra.JsonEntitiesFromSchemas
    with generic.JsonSchemas {

  case class ModelComposition(
    models: Map[String, Model],
    sharedStates: List[SharedState]
  )
  case class SharedState(
    model1Name: String,
    model1State: StateId,
    model2Name: String,
    model2State: StateId
  )

  // Expressions
  implicit lazy val exprBoolSchema: JsonSchema[math.Expr[Boolean]] =
    withExampleJsonSchema(
      schema = stringJsonSchema(format = Some("boolean-math")). xmapPartial(
        tryParseString("predicate")(str => math.Expr.predicate(str).get)
      )(
        _.prettyPrint()
      ),
      example = math.Expr.predicate("x > 8").get
    )
  implicit lazy val exprDblSchema: JsonSchema[math.Expr[Double]] =
    withExampleJsonSchema(
      schema = stringJsonSchema(format = Some("numeric-math")). xmapPartial(
        tryParseString("formula")(str => math.Expr.expression(str).get)
      )(
        _.prettyPrint()
      ),
      example = math.Expr.expression("2 * x - 3 / y + 9").get
    )
  implicit lazy val variableSchema: JsonSchema[math.Variable] =
    stringJsonSchema(format = Some("variable")).xmap(s => math.Variable(Symbol(s)))(_.s.name)
  implicit lazy val stateIdSchema: JsonSchema[StateId] =
    stringJsonSchema(format = Some("state-id")).xmap(StateId(_))(_.id)

  // Map schemas
  def customKeyMapSchema[K, V : JsonSchema](
    to: K => String,
    from: String => K
  ): JsonSchema[Map[K, V]] = mapJsonSchema[V].xmap(
    _.map { case (k,v) => (from(k), v) }.toMap
  )(
    _.map { case (k,v) => (to(k), v) }.toMap
  )
  implicit def mapEventIdSchema[A : JsonSchema]: JsonSchema[Map[EventId, A]] =
    customKeyMapSchema[EventId, A](_.id, EventId(_))
  implicit def mapStateIdSchema[A : JsonSchema]: JsonSchema[Map[StateId, A]] =
    customKeyMapSchema[StateId, A](_.id, StateId(_))
  implicit def mapVariableIdSchema[A : JsonSchema]: JsonSchema[Map[math.Variable, A]] =
    customKeyMapSchema[math.Variable, A](_.s.name, s => math.Variable(Symbol(s)))

  // Boilerplate schemas
  implicit lazy val inputPredSchema: JsonSchema[InputPredicate] = genericJsonSchema[InputPredicate]
  implicit lazy val outputPredSchema: JsonSchema[OutputPredicate] = genericJsonSchema[OutputPredicate]
  implicit lazy val stateSchema: JsonSchema[State] = genericJsonSchema[State]
  implicit lazy val eventSchema: JsonSchema[Event] = genericJsonSchema[Event]
  implicit lazy val modelSchema: JsonSchema[Model] = withExampleJsonSchema(
    schema = genericJsonSchema[Model],
    example = Model.sampleSir
  )
  implicit lazy val sampledTraceSchema: JsonSchema[math.SampledTrace] = withExampleJsonSchema(
    schema = genericJsonSchema[math.SampledTrace],
    example = math.SampledTrace(Vector(1, 1.1, 1.2, 1.3, 1.4, 1.5), Vector(1, 1.21, 1.44, 1.69, 1.96, 2.25))
  )
  implicit lazy val paletteItemSchema: JsonSchema[PaletteItem] = withExampleJsonSchema(
    schema = genericJsonSchema[PaletteItem],
    example = PaletteItem.infectPalette
  )
  implicit lazy val juliaGillespieInputsSchema: JsonSchema[JuliaGillespie.Inputs] = genericJsonSchema[JuliaGillespie.Inputs]
  implicit lazy val scipyIntegrateInputsSchema: JsonSchema[SciPyIntegrate.Inputs] = genericJsonSchema[SciPyIntegrate.Inputs]
  implicit lazy val juliaGillespieOutputsSchema: JsonSchema[JuliaGillespie.Outputs] = genericJsonSchema[JuliaGillespie.Outputs]
  implicit lazy val scipyIntegrateOutputsSchema: JsonSchema[SciPyIntegrate.Outputs] = genericJsonSchema[SciPyIntegrate.Outputs]

  implicit lazy val uiParameterSchema: JsonSchema[ui.Parameter] = genericJsonSchema[ui.Parameter]
  implicit lazy val uiNodePropsSchema: JsonSchema[ui.NodeProps] = genericJsonSchema[ui.NodeProps]
  implicit lazy val uiNodeSchema: JsonSchema[ui.Node] = genericJsonSchema[ui.Node]
  implicit lazy val uiLinkSchema: JsonSchema[ui.Link] = genericJsonSchema[ui.Link]
  implicit lazy val uiGraphSchema: JsonSchema[ui.Graph] = genericJsonSchema[ui.Graph]

  implicit lazy val sharedStateSchema: JsonSchema[SharedState] = genericJsonSchema[SharedState]
  implicit lazy val modelCompositionSchema: JsonSchema[ModelComposition] = genericJsonSchema[ModelComposition]

  implicit lazy val latexSchema: JsonSchema[LatexEquations] =
    withExampleJsonSchema(
      schema = genericJsonSchema[LatexEquations],
      example = LatexEquations(List(
        "\\frac{dX}{dt} = -Y",
        "\\frac{dY}{dt} = x - \\kappa Y ",
        "Y_0 = 1",
        "\\kappa = 0.1"
      ))
    )

  val root = path / "api"

  val getModel: Endpoint[Unit, Model] =
    endpoint(
      request = get(root / "appstate" / "model"),
      response = ok(jsonResponse[Model]),
      docs = EndpointDocs(
        summary = Some("fetch the currently loaded model"),
        tags = List("Internal Model")
      )
    )

  val postModel: Endpoint[Model, Unit] =
    endpoint(
      request = post(
        url = root / "appstate" / "model",
        entity = jsonRequest[Model]
      ),
      response = ok(emptyResponse),
      docs = EndpointDocs(
        summary = Some("overwrite the currently loaded model"),
        tags = List("Internal Model")
      )
    )

  val composeModels: Endpoint[ModelComposition, Model] =
    endpoint(
      request = post(
        url = root / "transient" / "model" / "compose",
        entity = jsonRequest[ModelComposition]
      ),
      response = ok(jsonResponse[Model]),
      docs = EndpointDocs(
        summary = Some("compose some models via state sharing and return the composed model"),
        tags = List("Internal Model")
      )
    )

  val postReset: Endpoint[Unit, Unit] =
    endpoint(
      request = post(
        url = root / "appstate" / "reset",
        entity = emptyRequest
      ),
      response = ok(emptyResponse),
      docs = EndpointDocs(
        summary = Some("clear application state (palettes, models, data, etc.)"),
        tags = List("General")
      )
    )

  val putDataTrace: Endpoint[(String, math.SampledTrace), Unit] =
    endpoint(
      request = put(
        url = root / "appstate" / "data-traces" /? qs[String]("name"),
        entity = jsonRequest[math.SampledTrace]
      ),
      response = response(Created, emptyResponse),
      docs = EndpointDocs(
        summary = Some("add a new data trace"),
        tags = List("Data Traces")
      )
    )

  val deleteDataTrace: Endpoint[String, Unit] =
    endpoint(
      request = delete(
        url = root / "appstate" / "data-traces" /? qs[String]("name"),
      ),
      response = ok(emptyResponse),  // TODO: not found case
      docs = EndpointDocs(
        summary = Some("remove a data trace"),
        tags = List("Data Traces")
      )
    )

  val getDataTrace: Endpoint[String, math.SampledTrace] =
    endpoint(
      request = get(
        url = root / "appstate" / "data-traces" /? qs[String]("name"),
      ),
      response = ok(jsonResponse[math.SampledTrace]),  // TODO: not found case
      docs = EndpointDocs(
        summary = Some("fetch a data trace"),
        tags = List("Data Traces")
      )
    )

  val listDataTraceNames: Endpoint[Option[Long], List[String]] =
    endpoint(
      request = get(
        url = root / "appstate" / "data-traces" / "names" /? qs[Option[Long]]("limit"),
      ),
      response = ok(jsonResponse[List[String]]),
      docs = EndpointDocs(
        summary = Some("fetch data trace names"),
        tags = List("Data Traces")
      )
    )

  def evalDataTraceQuery: Endpoint[String, math.SampledTrace] =
    endpoint(
      request = post(
        url = root / "appstate" / "data-traces" / "eval",
        entity = textRequest
      ),
      response = ok(jsonResponse[math.SampledTrace]), // TODO: error case
      docs = EndpointDocs(
        summary = Some("evaluate some data trace expression"),
        tags = List("Data Traces")
      )
    )

  def putUiModel: Endpoint[ui.Graph, Unit] =
    endpoint(
      request = put(
        url = root / "appstate" / "vdsol" / "model",
        entity = jsonRequest[ui.Graph]
      ),
      response = response(Created, emptyResponse),
      docs = EndpointDocs(
        summary = Some("update the current model using the VDSOL palette"),
        tags = List("VDSOL UI")
      )
    )

  def putPaletteItem: Endpoint[(String, PaletteItem), Unit] =
    endpoint(
      request = put(
        url = root / "appstate" / "vdsol" / "palette" /? qs[String]("name"),
        entity = jsonRequest[PaletteItem]
      ),
      response = response(Created, emptyResponse),
      docs = EndpointDocs(
        summary = Some("add a new palette item"),
        tags = List("VDSOL UI")
      )
    )

  val deletePaletteItem: Endpoint[String, Unit] =
    endpoint(
      request = delete(
        url = root / "appstate" / "vdsol" / "palette" /? qs[String]("name"),
      ),
      response = ok(emptyResponse),  // TODO: not found case
      docs = EndpointDocs(
        summary = Some("remove a palette item"),
        tags = List("VDSOL UI")
      )
    )

  val getPaletteItem: Endpoint[Option[String], List[PaletteItem]] =
    endpoint(
      request = get(
        url = root / "appstate" / "vdsol" / "palette" /? qs[Option[String]]("name"),
      ),
      response = ok(jsonResponse[List[PaletteItem]]),
      docs = EndpointDocs(
        summary = Some("fetch palette item(s)"),
        tags = List("VDSOL UI")
      )
    )

  val putDiffEqModel: Endpoint[LatexEquations, Unit] =
    endpoint(
      request = put(
        url = root / "appstate" / "diff-eq" / "model",
        entity = jsonRequest[LatexEquations]
      ),
      response = response(Created, emptyResponse),
      docs = EndpointDocs(
        summary = Some("update the current model using differential equations"),
        tags = List("Differential Equations UI")
      )
    )

  val scipyIntegrateBackend: Endpoint[SciPyIntegrate.Inputs, SciPyIntegrate.Outputs] =
    endpoint(
      request = post(
        url = root / "backends" / "scipy" / "integrate",
        entity = jsonRequest[SciPyIntegrate.Inputs]
      ),
      response = ok(jsonResponse[SciPyIntegrate.Outputs]), // TODO: errors
      docs = EndpointDocs(
        summary = Some("backend based on the SciPy ODE solver"),
        tags = List("Backends")
      )
    )

  val juliaGillespieBackend: Endpoint[JuliaGillespie.Inputs, JuliaGillespie.Outputs] =
    endpoint(
      request = post(
        url = root / "backends" / "julia" / "gillespie",
        entity = jsonRequest[JuliaGillespie.Inputs]
      ),
      response = ok(jsonResponse[JuliaGillespie.Outputs]), // TODO: errors
      docs = EndpointDocs(
        summary = Some("backend based on the Julia Gillespie solver"),
        tags = List("Backends")
      )
    )
}


trait AkkaHttpRoutes extends Routes
    with server.Endpoints
    with server.JsonEntitiesFromSchemas {

  import akka.http.scaladsl.server.Directives._

  val appState: AppState
  val requestId: AtomicLong
  implicit val ec: ExecutionContext

  private val modelRoutes =
    getModel.implementedBy { _ => appState.currentModel } ~
    postModel.implementedBy { model => appState.currentModel = model } ~
    composeModels.implementedBy { case ModelComposition(models, shared) =>
      val shrd = shared.map { case SharedState(m1,s1,m2,s2) => (m1 -> s1) -> (m2 -> s2) }
      Model.composeModels(models.toList, shrd)
    }

  private val resetRoute = postReset.implementedBy { _ => appState.reset() }

  private val dataTraceRoutes =
    putDataTrace.implementedBy { case (name, trace) =>
      appState.dataTraces += (name -> trace)
    } ~
    deleteDataTrace.implementedBy { name =>
      appState.dataTraces -= name
    } ~
    getDataTrace.implementedBy { name =>
      appState.dataTraces(name) match {
        case s: math.SampledTrace => s
        case p: math.PureFunc => p.sampleAt(collection.immutable.Range(0, 100, 1).toVector.map(_.toDouble))
      }
    } ~
    listDataTraceNames.implementedBy { limitOpt =>
      appState.dataTraces.keys.take(limitOpt.fold[Int](Int.MaxValue)(_.toInt)).toList
    } ~
    evalDataTraceQuery.implementedBy { query =>
      math.Trace(query, appState.dataTraces.toMap)
        .map {
          case s: math.SampledTrace => s
          case p: math.PureFunc => p.sampleAt(collection.immutable.Range(0, 100, 1).toVector.map(_.toDouble))
        }
        .get
    }

  private val vdsolRoutes =
    putUiModel.implementedBy { graph =>
      val model = graph.parse(appState.paletteItems).get
      appState.currentModel = model
    } ~
    putPaletteItem.implementedBy { case (name, paletteItem) =>
      appState.paletteItems += (name -> paletteItem)
    } ~
    deletePaletteItem.implementedBy { name =>
      appState.paletteItems -= name
    } ~
    getPaletteItem.implementedBy {
      case None => appState.paletteItems.values.toList
      case Some(name) => appState.paletteItems.get(name).toList
    }

  private val diffEqRoute =
    putDiffEqModel.implementedBy { case LatexEquations(equations) =>
      val model = LatexExtract.extractFromSource(equations).get
      appState.currentModel = model
    }

  private val backendRoutes =
    scipyIntegrateBackend.implementedByAsync { inputs =>
      SciPyIntegrate.routeComplete(
        appState.currentModel,
        appState,
        inputs,
        requestId.incrementAndGet()
      ).map(_.get)
    } ~
    juliaGillespieBackend.implementedByAsync { inputs =>
      JuliaGillespie.routeComplete(
        appState.currentModel,
        appState,
        inputs,
        requestId.incrementAndGet()
      ).map(_.get)
    }

  val routes =
    modelRoutes ~
    resetRoute ~
    dataTraceRoutes ~
    vdsolRoutes ~
    diffEqRoute ~
    backendRoutes
}

/** The OpenAPI "implementation" (docs) of our API
 *
 *  @param graph the Quine graph
 */
case object RoutesDocumentation
  extends Routes
    with openapi.Endpoints
    with openapi.JsonEntitiesFromSchemas {

  val api: openapi.model.OpenApi =
    openApi(
      openapi.model.Info(title = "AMIDOL", version = "1.0.0")
    )(
      getModel,
      postModel,
      composeModels,
      postReset,
      putDataTrace,
      deleteDataTrace,
      getDataTrace,
      listDataTraceNames,
      evalDataTraceQuery,
      putUiModel,
      putPaletteItem,
      deletePaletteItem,
      getPaletteItem,
      putDiffEqModel,
      scipyIntegrateBackend,
      juliaGillespieBackend
    )
}

case object DocumentationServer
  extends server.Endpoints
    with server.JsonEntitiesFromEncodersAndDecoders {

  import akka.http.scaladsl._
  import akka.http.scaladsl.model._
  import akka.http.scaladsl.server.Directives
  import akka.http.scaladsl.server.Directives._

  val routes = {
    val docEndpoint = endpoint(
      get(path / "docs" / "documentation.json"),
      ok(jsonResponse[openapi.model.OpenApi])
    )

    docEndpoint.implementedBy(_ => RoutesDocumentation.api) ~
    Directives.path("docs") {
      Directives.getFromResource("web/documentation.html")
    }
  }
}
