package amidol

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

import java.util.Base64

import akka.util.{ByteString, Timeout}

import endpoints.{Valid, Invalid, algebra, generic, openapi}
import endpoints.generic.{name, docs}
import endpoints.akkahttp.server

trait Routes
  extends algebra.Endpoints
    with algebra.JsonEntitiesFromSchemas
    with generic.JsonSchemas {

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
  implicit lazy val modelSchema: JsonSchema[Model] = genericJsonSchema[Model]
  implicit lazy val sampledTraceSchema: JsonSchema[math.SampledTrace] = genericJsonSchema[math.SampledTrace]
  implicit lazy val paletteItemSchema: JsonSchema[PaletteItem] = genericJsonSchema[PaletteItem]

  val getModel: Endpoint[Unit, Model] =
    endpoint(
      request = get(path / "appstate" / "model"),
      response = ok(jsonResponse[Model]),
      docs = EndpointDocs(
        summary = Some("fetch the currently loaded model"),
        tags = List("Model")
      )
    )

  val postModel: Endpoint[Model, Unit] =
    endpoint(
      request = post(
        url = path / "appstate" / "model",
        entity = jsonRequest[Model]
      ),
      response = ok(emptyResponse),
      docs = EndpointDocs(
        summary = Some("overwrite the currently loaded model"),
        tags = List("Model")
      )
    )

  val postReset: Endpoint[Unit, Unit] =
    endpoint(
      request = post(
        url = path / "appstate" / "reset",
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
        url = path / "appstate" / "data-traces" /? qs[String]("name"),
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
        url = path / "appstate" / "data-traces" /? qs[String]("name"),
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
        url = path / "appstate" / "data-traces" /? qs[String]("name"),
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
        url = path / "appstate" / "data-traces" / "names" /? qs[Option[Long]]("limit"),
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
        url = path / "appstate" / "data-traces" / "eval",
        entity = textRequest
      ),
      response = ok(jsonResponse[math.SampledTrace]), // TODO: error case
      docs = EndpointDocs(
        summary = Some("evaluate some data trace expression"),
        tags = List("Data Traces")
      )
    )

  def putPaletteItem: Endpoint[(String, PaletteItem), Unit] =
    endpoint(
      request = put(
        url = path / "appstate" / "palette" /? qs[String]("name"),
        entity = jsonRequest[PaletteItem]
      ),
      response = response(Created, emptyResponse),
      docs = EndpointDocs(
        summary = Some("add a new palette item"),
        tags = List("Palette")
      )
    )

  val deletePaletteItem: Endpoint[String, Unit] =
    endpoint(
      request = delete(
        url = path / "appstate" / "palette" /? qs[String]("name"),
      ),
      response = ok(emptyResponse),  // TODO: not found case
      docs = EndpointDocs(
        summary = Some("remove a palette item"),
        tags = List("Palette")
      )
    )

  val getPaletteItem: Endpoint[String, PaletteItem] =
    endpoint(
      request = get(
        url = path / "appstate" / "palette" /? qs[String]("name"),
      ),
      response = ok(jsonResponse[PaletteItem]),  // TODO: not found case
      docs = EndpointDocs(
        summary = Some("fetch a palette item"),
        tags = List("Palette")
      )
    )

  val listPaletteItems: Endpoint[Option[Long], List[PaletteItem]] =
    endpoint(
      request = get(
        url = path / "appstate" / "palette" / "names" /? qs[Option[Long]]("limit"),
      ),
      response = ok(jsonResponse[List[PaletteItem]]),
      docs = EndpointDocs(
        summary = Some("fetch loaded palette items"),
        tags = List("Palette")
      )
    )
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
      openapi.model.Info(title = "AMIDOL API", version = "1.0.0")
    )(
      getModel,
      postModel,
      postReset,
      putDataTrace,
      deleteDataTrace,
      getDataTrace,
      listDataTraceNames,
      evalDataTraceQuery,
      putPaletteItem,
      deletePaletteItem,
      getPaletteItem,
      listPaletteItems
    )
}

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

case object DocumentationServer
  extends server.Endpoints
    with server.JsonEntitiesFromEncodersAndDecoders {

  import akka.http.scaladsl._
  import akka.http.scaladsl.model._
  import akka.http.scaladsl.server.Directives

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
