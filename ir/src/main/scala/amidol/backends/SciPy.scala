package amidol.backends

import scala.util._
import amidol._
import spray.json._
import scala.sys.process._
import java.nio.file.Files
import java.nio.file.Paths

// Toy backend which generates (and runs) Python code that targets odeint from SciPy
object SciPy {

  def integrateSystem(
    model: Graph,
    constants: Map[Math.Variable,Double],
    boundary: Map[Math.Variable,Double],
    initialTime: Double,
    finalTime: Double,
    stepSize: Double
  ): Try[JsValue] = Try {
    
    val constantsSection = constants
      .map { case (v, d) => s"${v.prettyPrint()} = $d\n" }
      .mkString

    val timeRange = initialTime.until(finalTime, stepSize)
      .mkString("[",", ","]")

    val stateVariables: List[NodeId] = model.nodes.keys.toList

    val variables = stateVariables.map(i => model.nodes(i).stateVariable.prettyPrint())
    
    var derivatives: Map[NodeId,Math.Expr] = stateVariables.map(_ -> Math.Literal(0.0)).toMap
    for ((_, Edge(_, src, tgt, expr)) <- model.edges) {
      derivatives += src -> Math.Plus(derivatives(src), Math.Negate(expr))
      derivatives += tgt -> Math.Plus(derivatives(tgt),             expr )
    }
    val derivativeSection = derivatives
      .map { case (i, e) => s"    d${model.nodes(i).stateVariable.prettyPrint()}_ = ${e.prettyPrint()}\n" }
      .mkString

    val initial = stateVariables
      .map { i => boundary(model.nodes(i).stateVariable) }
      .mkString(", ")

    val src = s"""
                 |from scipy.integrate import odeint
                 |import json
                 |import numpy as np
                 |
                 |class NumpyEncoder(json.JSONEncoder):
                 |    def default(self, obj):
                 |        if isinstance(obj, np.ndarray):
                 |            return obj.tolist()
                 |        return json.JSONEncoder.default(self, obj)
                 |
                 |$constantsSection
                 |
                 |def deriv_(y_, t):
                 |    ${variables.mkString(", ")} = y_
                 |$derivativeSection
                 |    return ${variables.map(v => s"d${v}_").mkString(", ")}
                 |
                 |timeRange_ = $timeRange
                 |y0_ = $initial
                 |
                 |output = odeint(deriv_, y0_, timeRange_).T
                 |print(json.dumps(output, cls=NumpyEncoder))
                 |""".stripMargin
    Files.write(Paths.get("temp.py"), src.getBytes)

    
    val result = "python3 temp.py".!!

    result.parseJson
  }
}
