package dpla.publications

import com.example.UserRegistry.ActionPerformed

//#json-formats
import spray.json.DefaultJsonProtocol
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat, _}

//object JsonFormats  {
//  // import the default encoders for primitive types (Int, String, Lists etc)
//  import DefaultJsonProtocol._
//
//  implicit val publicationJsonFormat = jsonFormat2(Publication)
//  implicit val publicationsJsonFormat = jsonFormat1(Publications)
//
//  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
//}
//#json-formats

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

      val sourceUri: Option[String] = source.getFields("sourceUri") match {
        case Seq(JsString(value)) => Some(value)
        case _ => None
      }

      val title: Option[String] = source.getFields("title") match {
        case Seq(JsString(value)) => Some(value)
        case _ => None
      }

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

      val count: Int = json.asJsObject.getFields("hits") match {
        case Seq(obj: JsObject) =>
          obj.getFields("total") match {
            case Seq(obj: JsObject) =>
              obj.getFields("value") match {
                case Seq(JsNumber(value)) => value.intValue
                case unrecognized => deserializationError(s"json serialization error $unrecognized")
              }
            case unrecognized => deserializationError(s"json serialization error $unrecognized")
          }
        case unrecognized => deserializationError(s"json serialization error $unrecognized")
      }

      Publications(count, 1, 1)
    }
  }
}

case class Publications(
                         count: Int,
                         start: Int,
                         limit: Int
                         //                             docs: Array[Publication]
                       )

case class Publication(
                        sourceUri: Option[String],
                        title: Option[String]
                      )
