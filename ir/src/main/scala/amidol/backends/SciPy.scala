package amidol.backends

import scala.util._
import amidol._
import spray.json._
import scala.sys.process._
import java.nio.file.Files
import java.nio.file.Paths
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import scala.concurrent.{ExecutionContext, Future}

object SciPyIntegrate extends ContinuousInitialValue {
 
  val name: String = "SciPyIntegrate"
  val backendDescription: String = "Generates SciPy code to integrate an ODE system"
  
  // TODO: revisit this when we have discrete events
  def applicable(model: Graph): Boolean = true

  // TODO: do we want proper AST manipulation
  type Python = String

  def run(model: Graph, inputs: Inputs)(implicit ec: ExecutionContext): Future[Try[Outputs]] = Future {
    for {
      constants <- Try(inputs.constants.map { case (k,v) => math.Expr(k).flatMap(_.asVariable).get -> v })
      boundary  <- Try(inputs.boundary.map { case (k,v) => math.Expr(k).flatMap(_.asVariable).get -> v })
    
      timeRange: Seq[Double] = Range.BigDecimal(
        start = inputs.initialTime,
        end = inputs.finalTime,
        step = inputs.stepSize
      ).map(_.toDouble)

      // Set up the system of differential equations 
      stateVars: List[NodeId] = model.nodes.keys.toList
      derivatives: Map[NodeId, math.Expr] = {
        var builder = Map.empty[NodeId, math.Expr]

        for ((_, Edge(_, src, tgt, expr)) <- model.edges) {
          builder += src -> math.Plus(builder.getOrElse(src, 0.0), math.Negate(expr))
          builder += tgt -> math.Plus(builder.getOrElse(tgt, 0.0),             expr )
        }

        builder
      }

      // Pretty printing
      stateVarsStr   = stateVars.map(i => model.nodes(i).stateVariable.prettyPrint())
      derivativesStr = derivatives.toList.map(ie => s"d${model.nodes(ie._1).stateVariable.prettyPrint()}_ = ${ie._2.prettyPrint()}") 
      initialCondStr = stateVars.map(i => boundary(model.nodes(i).stateVariable))
      constantsStr   = constants.map(vd => s"${vd._1.prettyPrint()} = ${vd._2}")
      writeImageFile = inputs.savePlot

      // Python code
      plottingCode: Python = if (inputs.savePlot.isEmpty) "" else s"""
         |# How to plot
         |plt.title("SciPyIntegrate solution to continuous IVP", fontsize=12)
         |plt.xlabel("Time", fontsize=10)
         |plt.plot(timeRange_, np.transpose(output))
         |plt.legend(${stateVarsStr.map(v => '"' + v + '"').mkString("[", ", ", "]")})
         |plt.savefig('${inputs.savePlot.get}')
         |
         |""".stripMargin

      pythonCode: Python = s"""
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
      outputArrs <- {
        Files.write(Paths.get("tmp_script.py"), pythonCode.getBytes)
        println("Running `python3 tmp_script.py`...")
        Try("python3 tmp_script.py".!!) // blocks until script returns
      }

      // Parse the output back out
      nestedArrs <- Try(outputArrs.parseJson.convertTo[Seq[Seq[Double]]])

    } yield Outputs(
      variables = (stateVarsStr zip nestedArrs).toMap,
      times = timeRange
    )
  }
}

object SciPyLinearSteadyState extends ContinuousSteadyState {

  val name: String = "SciPyLinearSteadyState"
  val backendDescription: String = "Generates SciPy code to solve for equilibrium " +
                                   "points of a linear system"

  // TODO: revisit this when we have discrete events
  def applicable(model: Graph): Boolean = true

  // TODO: do we want proper AST manipulation
  type Python = String

  def run(model: Graph, inputs: Inputs)(implicit ec: ExecutionContext): Future[Try[Outputs]] = Future {

    // Extract the system
    val eqns: Map[math.Variable, math.Expr] = {
      var builder: Map[NodeId, math.Expr] = model.nodes.keys.map(_ -> (0: math.Expr)).toMap

      for ((_, Edge(_, src, tgt, expr)) <- model.edges) {
        builder += src -> math.Plus(builder.getOrElse(src, 0.0), math.Negate(expr))
        builder += tgt -> math.Plus(builder.getOrElse(tgt, 0.0),             expr )
      }

      builder
        .map { case (nId, exp) => model.nodes(nId).stateVariable -> exp }
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
        Files.write(Paths.get("tmp_script.py"), pythonCode.getBytes)
        println("Running `python3 tmp_script.py`...")
        Try("python3 tmp_script.py".!!) // blocks until script returns
      }

      // Parse the output back out
      arr <- Try(outputArr.parseJson.convertTo[Seq[Double]])
    } yield Outputs(Seq(Equilibrium(
      variables = (variables zip arr).map(vd => vd._1.prettyPrint() -> vd._2).toMap,
      stable = None
    )))
  }
}
