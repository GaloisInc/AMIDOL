package amidol

import spray.json._
import scala.io.Source
import scala.util.Try

/** Annotation that can be attached to an element in the ontology
 *
 *  @param itemType is the item a noun or a verb?
 *  @param colorRange the gradient from which the color will be picked
 *  @param svgImageSrc the source of the SVG to use
 */
case class OntologyAnnotation(
  itemType: ItemType,
  colorRange: ColorRange,
  svgImageSrc: String,
)
object OntologyAnnotation extends DefaultJsonProtocol {
  implicit val verbFormat = jsonFormat2(ItemType.Verb.apply)
  implicit val itemTypeFormat = new JsonFormat[ItemType] {
    def read(json: JsValue): ItemType = json match {
      case JsString("Noun") => ItemType.Noun
      case _ => verbFormat.read(json)
    }
    def write(obj: ItemType): JsValue = obj match {
      case ItemType.Noun => JsString("Noun")
      case verb: ItemType.Verb => verbFormat.write(verb)
    }
  }
  implicit val colorFormat = jsonFormat3(Color.apply)
  implicit val colorRangeFormat = jsonFormat2(ColorRange.apply)
  implicit val ontologyAnnotationFormat = jsonFormat3(OntologyAnnotation.apply)

  /** Go look on disk for an annotation with a given name */
  def getForName(name: String): Option[OntologyAnnotation] = Try({
    val src = Source.fromResource(s"annotations/$name").getLines.mkString("\n")
    src.parseJson.convertTo[OntologyAnnotation]
  }).toOption
}

/** RGB color gradient range */
case class ColorRange(
  start: Color,
  end: Color,
) {
  def sample(seed: Long): Color = {
    val s1: Double = (Math.abs(seed) % 76).toDouble / 76.0;
    val s2: Double = (Math.abs(seed) % 37).toDouble / 37.0;
    val s3: Double = (Math.abs(seed) % 21).toDouble / 21.0;
    Color(
      ((end.hue - start.hue) * s1 + start.hue).toLong,
      ((end.saturation - start.saturation) * s2 + start.saturation).toLong,
      ((end.luminosity - start.luminosity) * s2 + start.luminosity).toLong,
    )
  }
}

/** Represents a single RGB color */
case class Color(
  hue: Long,         // 0-255
  saturation: Long,  // percentage
  luminosity: Long,  // percentage
) {
  def render: String = s"hsl($hue, $saturation%, $luminosity%)"

  def +(other: Color) = Color(
    this.hue + other.hue,
    this.saturation + other.saturation,
    this.luminosity + other.luminosity,
  )

  def -(other: Color) = Color(
    this.hue - other.hue,
    this.saturation - other.saturation,
    this.luminosity - other.luminosity,
  )

  def *(scalar: Double) = Color(
    (this.hue * scalar).toLong,
    (this.saturation * scalar).toLong,
    (this.luminosity * scalar).toLong,
  )
}

/** Constraints imposed on a palette element */
sealed trait ItemType
object ItemType {
  case object Noun extends ItemType
  case class Verb(inputCount: Int, outputType: Int) extends ItemType
}

