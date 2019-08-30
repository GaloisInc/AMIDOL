package amidol

import scala.sys.process.Process
import scala.util.{Try, Failure, Success}
import scala.util.Random
import scala.concurrent.duration._

import java.io.File
import java.io.PrintWriter
import java.util.UUID
import amidol.OntologyDb.SnomedRecord

object JuliaExtract {

  def extractFromSource(
    juliaSource: String,
    name: String,
  ): Try[(ui.Graph, Map[String, PaletteItem])] = Try {

    println("Extracting Julia model...")

    // A file located here has the Julia source in it
    val fullSourceFilePath: String = {
      val file = File.createTempFile("julia_input", ".julia")
      val printer = new PrintWriter(file)
      printer.print(juliaSource.trim())
      printer.close()
      file.deleteOnExit()
      file.getAbsolutePath()
    }

    // Extract the AST
    val ast: String = Process(command = Seq(
      "julia", "scripts/dump_julia_ast.julia", fullSourceFilePath
    )).!!
    val sexprAst: JuliaSExpr = JuliaSExpr(ast).get

    // Turn it into a model
    val model: Model = sexprAst.extractModel()

    var nounCount = 0L
    def nextNounId() = {
      nounCount += 1
      nounCount
    }
    val paletteItemsNouns = model.states.map { case (sid, st) =>
      val groundingOpt = st.description match {
        case None => None
        case Some(desc) =>
          val done = OntologyDb
            .searchForFirst(
              searchTerm = desc,
              searchMatch = _.annotations.nonEmpty,
              haltOnMatch = true,
              limit = 500L,
              deadline = 10.seconds.fromNow
            )
            .flatMap { record: SnomedRecord =>
              OntologyAnnotation.getForName(record.annotations)
                .filter(_.itemType == ItemType.Noun)
                .map(a => (a.colorRange.sample(desc.hashCode), a.svgImageSrc))
            }
            .headOption
          done
      }

      sid -> PaletteItem(
        className = s"${name}_state${sid.id}_${nextNounId()}",
        `type` = "noun",
        sharedStates = Array[StateId](sid, sid),
        icon = groundingOpt.fold("images/unknown.png")(_._2),
        color = groundingOpt.map(_._1.render),
        backingModel = Model(
          states = Map(sid -> st.copy(
            initial_value = math.Variable('Initial)
          )),
          events = Map.empty,
          constants = Map(math.Variable('Initial) -> 0.0),
        )
      )
    }

    var verbCount = 0L
    def nextVerbId() = {
      verbCount += 1
      verbCount
    }
    val paletteItemsVerbs = model.events.map { case (eid, ev) =>

      val groundingOpt = ev.description match {
        case None => None
        case Some(desc) =>
          OntologyDb
            .searchForFirst(
              searchTerm = desc,
              searchMatch = _.annotations.nonEmpty,
              haltOnMatch = true,
              limit = 500L,
              deadline = 10.seconds.fromNow
            )
            .flatMap { record: SnomedRecord =>
              OntologyAnnotation.getForName(record.annotations)
                .filter(_.itemType == ItemType.Verb(1,1))
                .map(a => (a.colorRange.sample(desc.hashCode), a.svgImageSrc))
            }
            .headOption
      }

      val (in, out) = ev.output_predicate.transition_function.toList match {
        case List((s1: StateId, _: math.Negate), (s2: StateId, _)) => (s1, s2)
        case List((s1: StateId, _),              (s2: StateId, _)) => (s2, s1)
        case _  => throw JuliaExtractionError(s"Not sure how to extract event ${ev}")
      }

      val variablesUsed: Set[math.Variable] =
        ev.input_predicate.fold(Set.empty[math.Variable])(_.enabling_condition.variables()) ++
        ev.output_predicate.transition_function.values.map(_.variables()).fold(Set.empty[math.Variable])(_ ++ _) ++
        ev.rate.variables()

      eid -> PaletteItem(
        className = s"${name}_event${eid.id}_${nextVerbId()}",
        `type` = "verb",
        sharedStates = Array[StateId](in, out),
        icon = groundingOpt.fold("images/unknown.png")(_._2),
        color = groundingOpt.map(_._1.render),
        backingModel = Model(
          states = Map(
            in -> model.states(in),
            out -> model.states(out),
          ),
          events = Map(eid -> ev),
          constants = model.constants.filterKeys(variablesUsed.contains(_)),
        )
      )
    }

    val paletteItems =
      paletteItemsNouns.map { case (sid, pi) => sid.id -> pi } ++
      paletteItemsVerbs.map { case (eid, pi) => eid.id -> pi }

    val uiGraph = ui.Graph(
      nodes = paletteItems.map { case (id, pi) =>
        id -> ui.Node(
          id = id,
          label = pi.className,
          props = ui.NodeProps(
            className = pi.className,
            parameters = pi.backingModel.constants.toSeq.map { case (v,d) =>
              ui.Parameter(v.prettyPrint(), d)
            },
          ),
          x = Random.nextLong() % 100,
          y = Random.nextLong() % 100
        )
      },
      links = paletteItemsVerbs.iterator.flatMap { case (eid, pi) =>
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()
        List(
          id1 -> ui.Link(
            id = id1,
            from = pi.sharedStates(0).id,
            to = eid.id,
          ),
          id2 -> ui.Link(
            id = id2,
            from = eid.id,
            to = pi.sharedStates(1).id,
          )
        )
      }.toMap
    )
    println("Done.")

    (
      uiGraph,
      paletteItems.values.map(pi => pi.className -> pi).toMap
    )
  }

}

case class JuliaExtractionError(message: String) extends Exception(message)
