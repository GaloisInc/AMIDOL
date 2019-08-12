package amidol

import scala.sys.process.Process
import scala.util.{Try, Failure, Success}
import scala.util.Random

import java.io.File
import java.io.PrintWriter
import java.util.UUID

object JuliaExtract {

  def extractFromSource(
    juliaSource: String,
    name: String,
  ): Try[(ui.Graph, Map[String, PaletteItem])] = Try {

    // A file located here has the Julia source in it
    val fullSourceFilePath: String = {
      val file = File.createTempFile("julia_input", ".julia")
      val printer = new PrintWriter(file)
      printer.print(juliaSource.trim())
      printer.close()
      file.getAbsolutePath()
    }
    println(fullSourceFilePath)

    // Extract the AST
    val ast: String = Process(command = Seq(
      "julia", "scripts/dump_julia_ast.julia", fullSourceFilePath
    )).!!
    val sexprAst: JuliaSExpr = JuliaSExpr(ast).get

    // Turn it into a model
    val model: Model = sexprAst.extractModel()
    
    val paletteItemsNouns = model.states.map { case (sid, st) =>
      sid -> PaletteItem(
        className = s"${name}_state-${sid.id}_${Random.nextInt()}",
        `type` = "noun",
        sharedStates = Array[StateId](sid, sid),
        icon = "images/unknown.svg",
        backingModel = Model(
          states = Map(sid -> st),
          events = Map.empty,
          constants = model.constants,
        )
      )
    }
    val paletteItemsVerbs = model.events.map { case (eid, ev) =>
      val (in, out) = ev.output_predicate.transition_function.toList match {
        case List((s1: StateId, _: math.Negate), (s2: StateId, _)) => (s1, s2)
        case List((s1: StateId, _),              (s2: StateId, _)) => (s2, s1)
        case _  => throw JuliaExtractionError(s"Not sure how to extract event ${ev}")
      }
      eid -> PaletteItem(
        className = s"${name}_event-${eid.id}_${Random.nextInt()}",
        `type` = "verb",
        sharedStates = Array[StateId](in, out),
        icon = "images/unknown.svg",
        backingModel = Model(
          states = Map(
            in -> model.states(in),
            out -> model.states(out),
          ),
          events = Map(eid -> ev),
          constants = model.constants,
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
      links = paletteItemsVerbs.values.map { case pi =>
        val id = UUID.randomUUID().toString()
        id -> ui.Link(
          id = id,
          from = pi.sharedStates(0).id,
          to = pi.sharedStates(1).id,
        )
      }.toMap
    )

    (
      uiGraph,
      paletteItems.values.map(pi => pi.className -> pi).toMap
    )
  }

}

case class JuliaExtractionError(message: String) extends Exception(message)
