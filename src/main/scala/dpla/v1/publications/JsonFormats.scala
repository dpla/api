package dpla.v1.publications

import spray.json.DefaultJsonProtocol
import spray.json._

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

  implicit object BucketFormat extends RootJsonFormat[Bucket] {

    def read(json: JsValue): Bucket = {
      val root = json.asJsObject

      Bucket(
        key = readString(root, "key"),
        docCount = readInt(root, "doc_count")
      )
    }

    def write(b: Bucket): JsValue =
      JsObject(
        "term" -> b.key.toJson,
        "count" -> b.docCount.toJson
      ).toJson
  }

  /** In an ElasticSearch response, facets look something like this:
   *  {"aggregations": {"dataProvider": {"buckets": [...]}}, "sourceResource.publisher": {"buckets": [...]}}}
   *  You therefore have to read the keys of the "aggregation" object to get the facet field names.
   */
  implicit object FacetListFormat extends RootJsonFormat[FacetList] {

    def read(json: JsValue): FacetList = {
      val root: JsObject = json.asJsObject

      readObject(root) match {
        case Some(obj) =>
          val fieldNames: Seq[String] = obj.fields.keys.toSeq

          val facets: Seq[Facet] = fieldNames.map(fieldName =>
            Facet(
              field = fieldName,
              buckets = readObjectArray(root, fieldName, "buckets").map(_.toJson.convertTo[Bucket])
            )
          )

          FacetList(facets)

        case None =>
          // This should never happen
          FacetList(Seq[Facet]())
      }
    }

    def write(al: FacetList): JsValue = {
      var aggObject = JsObject()

      // Add a field to aggObject for each facet field
      al.facets.foreach(agg => {
        aggObject = JsObject(aggObject.fields + (agg.field -> JsObject("terms" -> agg.buckets.toJson)))
      })

      aggObject.toJson
    }
  }

  implicit object PublicationListFormat extends RootJsonFormat[PublicationList] {

    def read(json: JsValue): PublicationList = {
      val root = json.asJsObject

      PublicationList(
        count = readInt(root, "hits", "total", "value"),
        limit = None,
        start = None,
        docs = readObjectArray(root, "hits", "hits").map(_.toJson.convertTo[Publication]),
        facets = readObject(root, "aggregations").map(_.toJson.convertTo[FacetList])
      )
    }

    def write(pl: PublicationList): JsValue = {
      val base: JsObject = JsObject(
        "count" -> pl.count.toJson,
        "start" -> pl.start.toJson,
        "limit" -> pl.limit.toJson,
        "docs" -> pl.docs.toJson
      )

      // Add facets if there are any
      val complete: JsObject =
        if (pl.facets.nonEmpty) JsObject(base.fields + ("facets" -> pl.facets.toJson))
        else base

      complete.toJson
    }
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
                            docs: Seq[Publication],
                            facets: Option[FacetList]
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

case class FacetList(
                      facets: Seq[Facet]
                    )

case class Facet(
                  field: String,
                  buckets: Seq[Bucket]
                )

case class Bucket(
                   key: Option[String],
                   docCount: Option[Int]
                 )
