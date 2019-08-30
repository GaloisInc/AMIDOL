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
  def getForName(name: String): Option[OntologyAnnotation] = {
    val src = Source.fromResource(s"annotations/$name.template").getLines.mkString("\n")
    Try(src.parseJson.convertTo[OntologyAnnotation]).toOption
  }
}

/** RGB color gradient range */
case class ColorRange(
  start: Color,
  end: Color,
) {
  def sample(seed: Long): Color = {
    val position: Double = (Math.abs(seed) % 17).toDouble / 17.0;
    (end - start) * position + start
  }
}

/** Represents a single RGB color */
case class Color(
  red: Long,
  green: Long,
  blue: Long,
) {
  def render: String = f"#$red%.2f$green%.2f$blue%.2f"

  def +(other: Color) = Color(
    this.red + other.red,
    this.green + other.green,
    this.blue + other.blue,
  )

  def -(other: Color) = Color(
    this.red - other.red,
    this.green - other.green,
    this.blue - other.blue,
  )

  def *(scalar: Double) = Color(
    (this.red * scalar).toLong,
    (this.green * scalar).toLong,
    (this.blue * scalar).toLong,
  )
}

/** Constraints imposed on a palette element */
sealed trait ItemType
object ItemType {
  case object Noun extends ItemType
  case class Verb(inputCount: Int, outputType: Int) extends ItemType
}

