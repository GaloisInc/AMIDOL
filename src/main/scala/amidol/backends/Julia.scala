package amidol.backends

import scala.util._
import amidol._
import spray.json._
import scala.sys.process._
import java.nio.file.Files
import java.nio.file.Paths
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import scala.concurrent.{ExecutionContext, Future}

object JuliaGillespie extends ContinuousInitialValue {
 
  val name: String = "JuliaGillespie"
  val backendDescription: String = "Generates Julia code for a Gillespie model"
  
  // TODO: revisit this when we have discrete events
  def applicable(model: Model): Boolean = true

  // TODO: do we want proper AST manipulation
  type Julia = String

  override def run(
    model: Model,
    inputs: Inputs,
    requestId: Long
  )(implicit
    ec: ExecutionContext
  ): Future[Try[Outputs]] = Future {

    // Start by mapping all the constant and state variable names
    val constantsIdx: Map[math.Variable, Int] = model.constants.keys.zipWithIndex.map(kv => (kv._1, kv._2 + 1)).toMap
    val statesIdx: Map[StateId, Int] = model.states.keys.zipWithIndex.map(kv => (kv._1, kv._2 + 1)).toMap
    val stateVariablesIdx: Map[math.Variable, Int] = statesIdx.map { case (sId, idx) => model.states(sId).state_variable -> idx }

    // All variables are accesses into `p` and `u` arrays!!!
    val mappedModel = model.mapIds(
      stateIdFunc = identity,
      eventIdFunc = identity,
      variableFunc = (v: math.Variable) => {
        val p = constantsIdx.get(v).map(idx => s"p[$idx]")
        val u = stateVariablesIdx.get(v).map(idx => s"u[$idx]")
        math.Variable(Symbol((p orElse u).get))
      },
    )

    // Produce the rate and effect functions for a given event
    def getRateEvent(eventId: EventId): Julia = {
      val event = mappedModel.events(eventId)

      val effects: List[Julia] = event.output_predicate
        .transition_function
        .map { case (sId, expr) =>
          val state = mappedModel.states(sId).state_variable
          s"  integrator.${state.prettyPrint()} += (${expr.prettyPrint()})"
        }
        .toList

      s"""
       |# Stuff for ${eventId.id}
       |rate_${eventId.id}(u,p,t) = ${event.rate.prettyPrint()}
       |function event_${eventId.id}!(integrator)
       |${effects.mkString("\n")}
       |end
       |""".stripMargin
    }

    // Pretty printing
    val initialCondArray: Julia = mappedModel.states
      .toList
      .sortBy { case (sId, _) => statesIdx(sId) }
      .map { case (_, s) => s.initial_value.prettyPrint() }
      .mkString("[","; ","]")

    val stateVarsStr: List[String] = model.states
      .toList
      .sortBy { case (sId, _) => statesIdx(sId) }
      .map { case (_, s) => s.state_variable.prettyPrint() }
    
    val intialConstArray: Julia = model.constants
      .toList
      .sortBy { case (cv, _) => constantsIdx(cv) }
      .map { case (_, v) => v.toString }
      .mkString("(", ", ", ")")

    val writeImageFile = inputs.savePlot.fold("") { plotSavePath: String =>
      s"""plot(summ)
         |savefig("$plotSavePath")
         |""".stripMargin
    }

    val juliaCode: Julia = s"""
         |using DiffEqBiological
         |using DifferentialEquations
         |using Plots
         |using DiffEqMonteCarlo
         |using JSON
         |
         |${mappedModel.events.keys.map(eId => getRateEvent(eId)).mkString("\n")}
         |
         |p = $intialConstArray
         |u0 = $initialCondArray
         |tspan = (${inputs.initialTime}, ${inputs.finalTime})
         |jump_prob = JumpProblem(
         |  DiscreteProblem(u0, tspan, p),
         |  Direct(),
         |  ${model.events.keys.map(eId => s"ConstantRateJump(rate_${eId.id}, event_${eId.id}!)").mkString(",\n  ")}
         |)
         |monte_prob = MonteCarloProblem(jump_prob)
         |sol_monte = solve(monte_prob, num_monte=100)
         |summ = EnsembleSummary(sol_monte)
         |
         |$writeImageFile
         |
         |JSON.print((summ.u, summ.t))
         |""".stripMargin

    // Run the code
    for {
      outputArrs <- {
        Files.write(Paths.get("tmp_scripts", s"${requestId}_tmp_script.jl"), juliaCode.getBytes)
        println(s"Running `julia tmp_scripts/${requestId}_tmp_script.jl`...")
        Try(s"julia tmp_scripts/${requestId}_tmp_script.jl".!!) // blocks until script returns
      }

      // Parse the output back out
      (nestedArrsTransposed, times) <- Try(outputArrs.parseJson.convertTo[(Seq[Seq[Double]], Seq[Double])])
      nestedArrs = nestedArrsTransposed.transpose

    } yield Outputs(
      variables = (stateVarsStr zip nestedArrs).toMap,
      times = times
    )
  }
}
