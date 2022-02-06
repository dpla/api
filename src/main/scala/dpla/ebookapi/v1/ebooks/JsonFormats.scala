package dpla.ebookapi.v1.ebooks

import spray.json.{DefaultJsonProtocol, _}

/**
 * These formats are used for parsing ElasticSearch responses and mapping them to DPLA MAP.
 */
object JsonFormats extends DefaultJsonProtocol with JsonFieldReader {

  implicit object EbookFormat extends RootJsonFormat[Ebook] {

    def read(json: JsValue): Ebook = {
      val root: JsObject = json.asJsObject

      Ebook(
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

    def write(ebook: Ebook): JsValue = {

      filterIfEmpty(JsObject(
        "id" -> ebook.id.toJson,
        "ingestType" -> JsString("ebook"),
        "isShownAt" -> ebook.itemUri.toJson,
        "object" -> ebook.payloadUri.toJson,
        "provider" -> filterIfEmpty(JsObject(
          "@id" -> ebook.sourceUri.toJson,
        )),
        "sourceResource" -> filterIfEmpty(JsObject(
          "creator" -> ebook.author.toJson,
          "date" -> filterIfEmpty(JsObject(
            "displayDate" -> ebook.publicationDate.toJson
          )),
          "description" -> ebook.summary.toJson,
          "format" -> ebook.medium.toJson,
          "language" -> filterIfEmpty(JsObject(
            "name" -> ebook.language.toJson,
          )),
          "publisher" -> ebook.publisher.toJson,
          "subject" -> filterIfEmpty(JsObject(
            "name" -> ebook.genre.toJson
          )),
          "subtitle" -> ebook.subtitle.toJson,
          "title" -> ebook.title.toJson,
          "type" -> JsString("ebook")
        ))
      )).toJson
    }
  }

  implicit object SingleEbookFormat extends RootJsonFormat[SingleEbook] {

    def read(json: JsValue): SingleEbook = {
      SingleEbook(
        docs = Seq(json.convertTo[Ebook])
      )
    }

    def write(singleEbook: SingleEbook): JsValue = {
      JsObject(
        "count" -> JsNumber(1),
        "docs" -> singleEbook.docs.toJson
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

    def write(bucket: Bucket): JsValue =
      JsObject(
        "term" -> bucket.key.toJson,
        "count" -> bucket.docCount.toJson
      ).toJson
  }

  /** In an ElasticSearch response, facets look something like this:
   *  {"aggregations": {"provider.@id": {"buckets": [...]}}, "sourceResource.publisher": {"buckets": [...]}}}
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

    def write(facetList: FacetList): JsValue = {
      var aggObject = JsObject()

      // Add a field to aggObject for each facet field
      facetList.facets.foreach(agg => {
        aggObject = JsObject(aggObject.fields + (agg.field -> JsObject("terms" -> agg.buckets.toJson)))
      })

      aggObject.toJson
    }
  }

  implicit object EbookListFormat extends RootJsonFormat[EbookList] {

    def read(json: JsValue): EbookList = {
      val root = json.asJsObject

      EbookList(
        count = readInt(root, "hits", "total", "value"),
        limit = None,
        start = None,
        docs = readObjectArray(root, "hits", "hits").map(_.toJson.convertTo[Ebook]),
        facets = readObject(root, "aggregations").map(_.toJson.convertTo[FacetList])
      )
    }

    def write(ebookList: EbookList): JsValue = {
      val base: JsObject = JsObject(
        "count" -> ebookList.count.toJson,
        "start" -> ebookList.start.toJson,
        "limit" -> ebookList.limit.toJson,
        "docs" -> ebookList.docs.toJson
      )

      // Add facets if there are any
      val complete: JsObject =
        if (ebookList.facets.nonEmpty) JsObject(base.fields + ("facets" -> ebookList.facets.toJson))
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
