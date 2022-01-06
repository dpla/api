package dpla.publications

import spray.json.DefaultJsonProtocol
import spray.json.{JsString, JsValue, RootJsonFormat, _}

object JsonFormats extends DefaultJsonProtocol with JsonFieldReader {

  implicit object PublicationFormat extends RootJsonFormat[Publication] {

    def read(json: JsValue): Publication = {
      val root: JsObject = json.asJsObject

      Publication(
        author = readStringArray(root, "_source", "author"),
        genre = readStringArray(root, "_source", "genre"),
        id = readString(root, "_id"),
        itemUri = readString(root, "_source", "itemUri"),
        medium = readStringArray(root, "_source", "medium"),
        language = readStringArray(root, "_source", "language"),
        payloadUri = readStringArray(root, "_source", "payloadUri"),
        publisher = readStringArray(root, "_source", "publisher"),
        publicationDate = readStringArray(root, "_source", "publicationDate"),
        sourceUri = readString(root, "_source", "sourceUri"),
        subtitle = readStringArray(root, "_source", "subtitle"),
        summary = readStringArray(root, "_source", "summary"),
        title = readStringArray(root, "_source", "title")
      )
    }

    def write(pub: Publication): JsValue =

      filterEmptyFields(JsObject(
        "count" -> JsNumber(1),
        "docs" -> filterEmptyFields(JsObject(
          "id" -> pub.id.toJson,
          "dataProvider" -> pub.sourceUri.toJson,
          "ingestType" -> JsString("ebook"),
          "isShownAt" -> pub.itemUri.toJson,
          "object" -> pub.payloadUri.toJson,
          "sourceResource" -> filterEmptyFields(JsObject(
            "creator" -> pub.author.toJson,
            "date" -> filterEmptyFields(JsObject(
              "displayDate" -> pub.publicationDate.toJson
            )),
            "description" -> pub.summary.toJson,
            "format" -> pub.medium.toJson,
            "language" -> filterEmptyFields(JsObject(
              "name" -> pub.language.toJson,
            )),
            "publisher" -> pub.publisher.toJson,
            "subject" -> filterEmptyFields(JsObject(
              "name" -> pub.genre.toJson
            )),
            "subtitle" -> pub.subtitle.toJson,
            "title" -> pub.title.toJson,
            "type" -> JsString("ebook")
          ))
        ))
      )).toJson
  }

  implicit object PublicationsFormat extends RootJsonFormat[Publications] {

    def read(json: JsValue): Publications = {

      val root = json.asJsObject

      Publications(
        count = readInt(root, "hits", "total", "value"),
        limit = 0,
        start = 0
      )
    }

    def write(pl: Publications): JsValue =
      JsObject(
        "count" -> pl.count.toJson,
        "start" -> pl.start.toJson,
        "limit" -> pl.limit.toJson
      ).toJson
  }

  /** Methods for writing JSON **/

  // Filter out fields with null values or empty arrays.
  def filterEmptyFields(obj: JsObject): JsObject = {
    val filtered: Map[String, JsValue] = obj.fields.filterNot(_._2 match {
      case JsNull => true
      case JsArray(values) =>
        if (values.isEmpty) true else false
      case _ => false
    })

    JsObject(filtered)
  }
}

/** Case classes for reading ElasticSearch responses **/

case class Publications(
                         count: Option[Int],
                         limit: Int,
                         start: Int
                         //                             docs: Array[Publication]
                       )

case class Publication(
                        author: Seq[String],
                        genre: Seq[String],
                        id: Option[String],
                        itemUri: Option[String],
                        language: Seq[String],
                        medium: Seq[String],
                        payloadUri: Seq[String],
                        publisher: Seq[String],
                        publicationDate: Seq[String],
                        sourceUri: Option[String],
                        subtitle: Seq[String],
                        summary: Seq[String],
                        title: Seq[String]
                      )
