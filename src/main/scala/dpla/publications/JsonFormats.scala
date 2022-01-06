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

    def write(pub: Publication): JsValue = {

      filterIfEmpty(JsObject(
        "id" -> pub.id.toJson,
        "dataProvider" -> pub.sourceUri.toJson,
        "ingestType" -> JsString("ebook"),
        "isShownAt" -> pub.itemUri.toJson,
        "object" -> pub.payloadUri.toJson,
        "sourceResource" -> filterIfEmpty(JsObject(
          "creator" -> pub.author.toJson,
          "date" -> filterIfEmpty(JsObject(
            "displayDate" -> pub.publicationDate.toJson
          )),
          "description" -> pub.summary.toJson,
          "format" -> pub.medium.toJson,
          "language" -> filterIfEmpty(JsObject(
            "name" -> pub.language.toJson,
          )),
          "publisher" -> pub.publisher.toJson,
          "subject" -> filterIfEmpty(JsObject(
            "name" -> pub.genre.toJson
          )),
          "subtitle" -> pub.subtitle.toJson,
          "title" -> pub.title.toJson,
          "type" -> JsString("ebook")
        ))
      )).toJson
    }
  }

  implicit object SinglePublicationFormat extends RootJsonFormat[SinglePublication] {

    def read(json: JsValue): SinglePublication = {
      SinglePublication(
        docs = Seq(json.convertTo[Publication])
      )
    }

    def write(sp: SinglePublication): JsValue = {
      JsObject(
        "count" -> JsNumber(1),
        "docs" -> sp.docs.toJson
      ).toJson
    }
  }

  implicit object PublicationListFormat extends RootJsonFormat[PublicationList] {

    def read(json: JsValue): PublicationList = {
      val root = json.asJsObject

      // TODO add limit and start when working on pagination ticket
      PublicationList(
        count = readInt(root, "hits", "total", "value"),
        limit = None,
        start = None,
        docs = readObjectArray(root, "hits", "hits").map(_.toJson.convertTo[Publication])
      )
    }

    def write(pl: PublicationList): JsValue =
      JsObject(
        "count" -> pl.count.toJson,
        "start" -> pl.start.toJson,
        "limit" -> pl.limit.toJson,
        "docs" -> pl.docs.toJson
      ).toJson
  }

  /** Methods for writing JSON **/

  // Filter out fields whose values are: null, empty array, or object with all empty fields.
  def filterIfEmpty(obj: JsObject): JsObject = {

    def filterFields(fields: Map[String, JsValue]): Map[String, JsValue] =
      fields.filterNot(_._2 match {
        case JsNull => true
        case JsArray(values) =>
          if (values.isEmpty) true else false
        case JsObject(fields) =>
          if (filterFields(fields).isEmpty) true else false
        case _ => false
      })

    val filtered = filterFields(obj.fields)
    JsObject(filtered)
  }
}

/** Case classes for reading ElasticSearch responses **/

case class SinglePublication(
                              docs: Seq[Publication]
                            )

case class PublicationList(
                            count: Option[Int],
                            limit: Option[Int],
                            start: Option[Int],
                            docs: Seq[Publication]
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
