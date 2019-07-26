package amidol.backends

import scala.util._
import amidol._
import spray.json._
import scala.sys.process._
import java.nio.file.Files
import java.nio.file.Paths
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import scala.concurrent.{ExecutionContext, Future}
import java.text.SimpleDateFormat
import java.util.Date

object SciPyIntegrate extends ContinuousInitialValue {
 
  val name: String = "SciPyIntegrate"
  val backendDescription: String = "Generates SciPy code to integrate an ODE system"
  
  // TODO: revisit this when we have discrete events
  def applicable(model: Model): Boolean = true

  // TODO: do we want proper AST manipulation
  type Python = String

  override def run(
    model: Model,
    inputs: Inputs,
    requestId: Long
  )(implicit
    ec: ExecutionContext
  ): Future[Try[Outputs]] = Future {
    val timeRange: Vector[Double] = Range.BigDecimal(
      start = inputs.initialTime,
      end = inputs.finalTime,
      step = inputs.stepSize
    ).map(_.toDouble).toVector

    // Set up the system of differential equations 
    val states: List[State] = model.states.values.toList
    val derivatives: Map[StateId, Python] = {
      val builder = collection.mutable.Map.empty[StateId, String]

      for ((_,  Event(rate, inputPredicate, outputPredicate, _)) <- model.events) {

        if (inputPredicate != None) {
          throw new Exception("Don't know how to handle input predicates yet")
        }

        for ((stateId, effect) <- outputPredicate.transition_function) {
          val term = math.Mult(rate, effect)
          builder(stateId) = builder.get(stateId) match {
            case None => term.prettyPrint()
            case Some(existing) =>
              inputPredicate match {
                case None => s"${term.prettyPrint()} + $existing"
                case Some(i) => s"(${term.prettyPrint()} if ${i.enabling_condition.prettyPrint()} else 0) + $existing"
              }
          }
        }
      }

      builder.toMap
    }

    // Pretty printing
    val stateVarsStr   = states.map(s => s.state_variable.prettyPrint())
    val derivativesStr = derivatives.toList.map(ie => s"d${model.states(ie._1).state_variable.prettyPrint()}_ = ${ie._2}") 
    val initialCondStr = states.map(s => s.initial_value.prettyPrint())
    val constantsStr   = model.constants.map(vd => s"${vd._1.prettyPrint()} = ${vd._2}")
    val writeImageFile = inputs.savePlot

    // Python code
    val plottingCode: Python = if (inputs.savePlot.isEmpty) "" else s"""
         |# How to plot
         |plt.title("SciPyIntegrate solution to continuous IVP", fontsize=12)
         |plt.xlabel("Time", fontsize=10)
         |plt.plot(timeRange_, np.transpose(output))
         |plt.legend(${stateVarsStr.map(v => '"' + v + '"').mkString("[", ", ", "]")})
         |plt.savefig('${inputs.savePlot.get}')
         |
         |""".stripMargin

    val pythonCode: Python = s"""
         |from scipy.integrate import odeint
         |import json
         |import numpy as np
         |import matplotlib.pyplot as plt
         |
         |# This is so that we can call "json.dumps" on Numpy arrays
         |class NumpyEncoder(json.JSONEncoder):
         |    def default(self, obj):
         |        if isinstance(obj, np.ndarray):
         |            return obj.tolist()
         |        return json.JSONEncoder.default(self, obj)
         |
         |# User defined constants
         |${constantsStr.mkString("\n")}
         |
         |# The ODE system
         |def deriv_(y_, t_):
         |    ${stateVarsStr.mkString(", ")} = y_
         |    ${derivativesStr.mkString("\n    ")}
         |    return ${stateVarsStr.map(v => s"d${v}_").mkString(", ")}
         |
         |# Boundary conditions and setup
         |timeRange_ = ${timeRange.mkString("[ ",", "," ]")}
         |y0_ = ${initialCondStr.mkString(", ")}
         |output = odeint(deriv_, y0_, timeRange_).T
         |
         |$plottingCode
         |
         |print(json.dumps(output, cls=NumpyEncoder))
         |""".stripMargin

    // Run the code
    for {
      outputArrs <- {
        Files.write(Paths.get("tmp_scripts", s"${requestId}_tmp_script.py"), pythonCode.getBytes)
        println(s"Running `python3 tmp_scripts/${requestId}_tmp_script.py`...")
        Try(s"python3 tmp_scripts/${requestId}_tmp_script.py".!!) // blocks until script returns
      }

      // Parse the output back out
      nestedArrs <- Try(outputArrs.parseJson.convertTo[Seq[Vector[Double]]])

    } yield {
      val traces = stateVarsStr zip nestedArrs
      val date = new SimpleDateFormat("dd-MM-yy:HH:mm:SS").format(new Date())
      Main.AppState.dataTraces ++= traces.map { case (traceName, traceData) =>
        s"${date}_${traceName}_scipy_${requestId}" -> (timeRange, traceData)
      }
      Outputs(
        variables = traces.toMap,
        times = timeRange
      )
    }
  }
}
/*
object SciPyLinearSteadyState extends ContinuousSteadyState {

  val name: String = "SciPyLinearSteadyState"
  val backendDescription: String = "Generates SciPy code to solve for equilibrium " +
                                   "points of a linear system"

  // TODO: revisit this when we have discrete events
  def applicable(model: Model): Boolean = true

  // TODO: do we want proper AST manipulation
  type Python = String

  override def run(
    model: Model,
    constants: Map[String, Double],
    boundary:  Map[String, Double],
    inputs: Inputs,
    requestId: Long
  )(implicit
    ec: ExecutionContext
  ): Future[Try[Outputs]] = Future {

    // Extract the system
    val eqns: Map[math.Variable, math.Expr[Double]] = {
      var builder: Map[StateId, math.Expr[Double]] = model.states.keys.map(_ -> (0: math.Expr[Double])).toMap

      for ((_, verb) <- model.events) {
        verb match {
          case Conserved(_, src, tgt, expr) =>
            builder += src -> math.Plus(builder.getOrElse(src, 0.0), math.Negate(expr))
            builder += tgt -> math.Plus(builder.getOrElse(tgt, 0.0),             expr )

          case Unconserved(_, src, tgt, exprOut, exprIn) =>
            builder += src -> math.Plus(builder.getOrElse(src, 0.0), math.Negate(exprOut))
            builder += tgt -> math.Plus(builder.getOrElse(tgt, 0.0),             exprIn )

          case Source(_, tgt, exprIn) =>
            builder += tgt -> math.Plus(builder.getOrElse(tgt, 0.0),             exprIn )

          case Sink(_, src, exprOut) =>
            builder += src -> math.Plus(builder.getOrElse(src, 0.0), math.Negate(exprOut))
        }
      }

      builder
        .map { case (nId, exp) => model.states(nId).stateVariable -> exp }
        .toMap
    }

    for {
      math.LinearSystem(variables, coeffs) <- Try(math.Linear.fromEquations(eqns).get)
      n = variables.length   

      // Python code
      pythonCode: Python = s"""
         |from numpy.linalg import solve
         |import json
         |import numpy as np
         |
         |# This is so that we can call "json.dumps" on Numpy arrays
         |class NumpyEncoder(json.JSONEncoder):
         |    def default(self, obj):
         |        if isinstance(obj, np.ndarray):
         |            return obj.tolist()
         |        return json.JSONEncoder.default(self, obj)
         |
         |# Solve the matrix equation Ax = x using (A - I)x = 0
         |A = np.array(${coeffs.map(_.mkString("[",",","]")).mkString("[",",","]")})
         |I = np.identity($n)
         |zero = np.zeros($n)
         |x = solve(A - I, zero)
         |
         |print(json.dumps(x, cls=NumpyEncoder))
         |""".stripMargin

      // Run the code
      outputArr <- {
        Files.write(Paths.get("tmp_scripts", s"${requestId}_tmp_script.py"), pythonCode.getBytes)
        println(s"Running `python3 tmp_scripts/${requestId}_tmp_script.py`...")
        Try(s"python3 tmp_scripts/${requestId}_tmp_script.py".!!) // blocks until script returns
      }

      // Parse the output back out
      arr <- Try(outputArr.parseJson.convertTo[Seq[Double]])
    } yield Outputs(Seq(Equilibrium(
      variables = (variables zip arr).map(vd => vd._1.prettyPrint() -> vd._2).toMap,
      stable = None
    )))
  }
}
*/
