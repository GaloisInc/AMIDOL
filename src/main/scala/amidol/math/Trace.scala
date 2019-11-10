package amidol.math

import amidol.AmidolParser
import scala.util.parsing.combinator._
import scala.util.parsing.input._
import scala.util._
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import scala.collection.Searching.{Found, InsertionPoint, SearchResult}

sealed trait Trace {

  def mapValues(valueFunc: Double => Double): Trace
  def mapDomain(domainFunc: Double => Double): Trace


  /** Figure out what value you have at a given domain point */
  def apply(at: Double): Double

  /** Combine two traces piece-wise, interpolating at intermediate data points
   *  when required
   */
  def combine(
    other: Trace,
    op: (Double, Double) => Double
  ): Trace = (this, other) match {
    case (PureFunc(f1), PureFunc(f2)) =>
      PureFunc(x => f1(x) + f2(x))

    case (PureFunc(f1), SampledTrace(d,v)) =>
      SampledTrace(d, (d zip v).map { case (x,y) => op(y, f1(x)) })

    case (SampledTrace(d,v), PureFunc(f2)) =>
      SampledTrace(d, (d zip v).map { case (x,y) => op(y, f2(x)) })

    case (s1: SampledTrace, s2: SampledTrace) =>
      s1.combinedSampled(s2)
  }

}


object Trace extends AmidolParser with SprayJsonSupport with DefaultJsonProtocol {

  // Simple arithmetic grammar with a packrat parser (cuz it's fast and I like my left recursion)
  def traceParser(traces: Map[String, Trace]): PackratParser[Trace] = {

    lazy val atom: PackratParser[Trace] =
      ( "-" ~> atom                        ^^ { t => t.mapValues(x => -x) }
      | "time()"                           ^^ { _ => PureFunc(x => x) }
      | floatingPointNumber                ^^ { s => PureFunc(_ => s.toDouble)  }
      | raw"(?U)\p{L}[\p{L}\p{No}_]*".r    ^^ { v => traces(v)  }
      | raw"`[^`]+`".r                     ^^ { v => traces(v.tail.init) }
      | "(" ~> term <~ ")"
      )

    lazy val factor: PackratParser[Trace] =
      ( factor ~ "*" ~ atom                ^^ { case (l ~ _ ~ r) => l.combine(r, _ * _) }
      | factor ~ "/" ~ atom                ^^ { case (l ~ _ ~ r) => l.combine(r, _ / _) }
      | atom
      )

    lazy val term: PackratParser[Trace] =
      ( term ~ "+" ~ factor                ^^ { case (l ~ _ ~ r) => l.combine(r, _ + _) }
      | term ~ "-" ~ factor                ^^ { case (l ~ _ ~ r) => l.combine(r, _ - _) }
      | factor
      )

    term
  }

  // Parse a trace expression from a string
  def apply(input: String, traces: Map[String, Trace]): Try[Trace] = runParser(traceParser(traces), input)

}


case class PureFunc(
  func: Double => Double
) extends Trace {

  def mapValues(valueFunc: Double => Double): Trace =
    PureFunc(x => valueFunc(func(x)))

  def mapDomain(domainFunc: Double => Double): Trace =
    PureFunc(x => func(domainFunc(x)))

  def apply(at: Double): Double = func(at)

  /** Post-condition: the measured trace has all of the domain points in the
   *  argument */
  def sampleAt(domain: Vector[Double]): SampledTrace = SampledTrace(
    domain,
    domain.map(func),
  )
}

case class SampledTrace(
  domain: Vector[Double],
  values: Vector[Double],
) extends Trace {
  require(
    domain.size == values.size,
    "domain and co-domain must have the same length"
  )

  def mapValues(func: Double => Double) = SampledTrace(domain, values.map(func))
  def mapDomain(func: Double => Double) = SampledTrace(domain.map(func), values)

  def apply(at: Double): Double =
    domain.search(at) match {
      case Found(idx) => values(idx)
      case InsertionPoint(idx) if idx == 0 || idx == domain.size => 0.0
      case InsertionPoint(idx) =>
        val x0 = domain(idx - 1)
        val x1 = domain(idx)

        val y0 = values(idx - 1)
        val y1 = values(idx)

        y0 + (at - x0) * (y1 - y0) / (x1 - x0)
    }

  def combinedSampled(other: SampledTrace): SampledTrace = {
    
    val x1 = domain;
    val x2 = other.domain;
    val y1 = values;
    val y2 = other.values;

    // Summed trace
    val x3 = Vector.newBuilder[Double]
    val y3 = Vector.newBuilder[Double]

    var i1 = 0  // index through trace1
    var i2 = 0  // index through trace2
    while (i1 < x1.size || i2 < x2.size) {
      if (!(i1 < x1.size)) {
        // We've run out of entries in `trace1`...
        x3 += x2(i2)
        y3 += y2(i2)
        i2 += 1
		  } else if (!(i2 < x2.size)) {
        // We've run out of entries in `trace2`...
        x3 += x1(i1)
        y3 += y1(i1)
        i1 += 1
		  } else if (x1(i1) == x2(i2)) {
        // They share the same `x` value, so just add the `y` values
        x3 += x1(i1)
        y3 += y1(i1) + y2(i2);
        i2 += 1
        i1 += 1
		  } else if (x1(i1) < x2(i2) && i2 == 0) {
        // We've not started values in `trace2`
        x3 += x1(i1)
        y3 += y1(i1)
        i1 += 1
		  } else if (x2(i2) < x1(i1) && i1 == 0) {
        // We've not started values in `trace1`
        x3 += x2(i2)
        y3 += y2(i2)
        i2 += 1
		  } else if (x1(i1) < x2(i2)) {
        // interpolate the value for `trace2`
        val mult = (x1(i1) - x2(i2-1)) / (x2(i2) - x2(i2-1))
        val y2_interp = (y2(i2) - y2(i2-1)) * mult + y2(i2-1)
        x3 += x1(i1)
        y3 += y1(i1) + y2_interp
        i1 += 1
		  } else if (x2(i2) < x1(i1)) {
        // interpolate the value for `trace1`
        val mult = (x2(i2) - x1(i1-1)) / (x1(i1) - x1(i1-1))
        val y1_interp = (y1(i1) - y1(i1-1)) * mult + y1(i1-1)
        x3 += x2(i2)
        y3 += y2(i2) + y1_interp
        i2 += 1
		  }
	  }

    SampledTrace(x3.result(), y3.result())
  }

}

