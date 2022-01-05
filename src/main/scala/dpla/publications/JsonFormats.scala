package dpla.publications

import spray.json.DefaultJsonProtocol
import spray.json.{JsString, JsValue, RootJsonFormat, _}

object JsonFormats extends DefaultJsonProtocol {

  implicit object PublicationFormat extends RootJsonFormat[Publication] {
    def write(pub: Publication): JsValue =
      JsObject(
        "sourceUri" -> pub.sourceUri.toJson,
        "title" -> pub.title.toJson
      ).toJson

    def read(json: JsValue): Publication = {

      val source: JsObject = json.asJsObject.getFields("_source") match {
        case Seq(obj: JsObject) => obj
        case unrecognized => deserializationError(s"json serialization error $unrecognized")
      }

      val sourceUri: Option[String] = getStringOpt(source, "sourceUri")

      val title: Seq[String] = getStringSeq(source, "title")

      Publication(sourceUri, title)
    }
  }

  implicit object PublicationsFormat extends RootJsonFormat[Publications] {
    def write(pl: Publications): JsValue =
      JsObject(
        "count" -> pl.count.toJson,
        "start" -> pl.start.toJson,
        "limit" -> pl.limit.toJson
      ).toJson

    def read(json: JsValue): Publications = {

      val count: Option[Int] = json.asJsObject.getFields("hits") match {
        case Seq(obj: JsObject) =>
          obj.getFields("total") match {
            case Seq(obj: JsObject) => getIntOpt(obj, "value")
            case unrecognized => deserializationError(s"json serialization error $unrecognized")
          }
        case unrecognized => deserializationError(s"json serialization error $unrecognized")
      }

      Publications(count, 0, 0)
    }
  }

  def getStringOpt(obj: JsObject, fieldName: String): Option[String] =
    obj.getFields(fieldName) match {
      case Seq(JsString(value)) => Some(value)
      case _ => None
    }

  def getStringSeq(obj: JsObject, fieldName: String): Seq[String] =
    obj.getFields(fieldName) match {
      case Seq(JsArray(vector)) => vector.flatMap(_ match {
        case JsString(value) => Some(value)
        case _ => None
      })
      case _ => Seq[String]()
    }

  def getIntOpt(obj: JsObject, fieldName: String): Option[Int] =
    obj.getFields(fieldName) match {
      case Seq(JsNumber(value)) => Some(value.intValue)
      case _ => None
    }
}

case class Publications(
                         count: Option[Int],
                         start: Int,
                         limit: Int
                         //                             docs: Array[Publication]
                       )

case class Publication(
                        sourceUri: Option[String],
                        title: Seq[String]
                      )
