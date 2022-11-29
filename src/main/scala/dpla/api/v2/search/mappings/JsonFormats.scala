package dpla.api.v2.search.mappings

import dpla.api.v2.search.models.DPLAMAPFields
import dpla.api.v2.search._
import spray.json._

/**
 * These formats are used for parsing ElasticSearch responses and mapping them
 * to DPLA MAP.
 */
object JsonFormats extends DefaultJsonProtocol with JsonFieldReader
  with DPLAMAPFields {

  implicit object BucketFormat extends RootJsonFormat[Bucket] {

    def read(json: JsValue): Bucket = {
      val root = json.asJsObject

      val bucket = Bucket(
        key = readString(root, "key"),
        keyAsString = readString(root, "key_as_string"),
        docCount = readInt(root, "doc_count"),
        to = readInt(root, "to"),
        from = readInt(root, "from")
      )

      // Do not include key for geo_distance or date_histogram buckets
      if (bucket.from.nonEmpty) bucket.copy(key = None)
      else if (bucket.keyAsString.nonEmpty) bucket.copy(key = None)
      else bucket
    }

    def write(bucket: Bucket): JsValue = {

      filterIfEmpty(JsObject(
        "term" -> bucket.key.toJson,
        "time" -> bucket.keyAsString.toJson,
        "count" -> bucket.docCount.toJson,
        "to" -> bucket.to.toJson,
        "from" -> bucket.from.toJson
      )).toJson
    }
  }

  /**
   * In an ElasticSearch response, facets look something like this:
   * {"aggregations": {"provider.@id": {"buckets": [...]}}}
   * You therefore have to read the keys of the "aggregation" object to get the
   * facet field names.
   */
  implicit object FacetListFormat extends RootJsonFormat[FacetList] {

    def read(json: JsValue): FacetList = {
      val root: JsObject = json.asJsObject

      readObject(root) match {
        case Some(obj) =>
          val fieldNames: Seq[String] = obj.fields.keys.toSeq

          val facets: Seq[Facet] = fieldNames.map(fieldName => {
            val `type` =
              if (coordinatesField.map(_.name).contains(fieldName))
                "geo_distance"
              else if (dateFields.map(_.name).contains(fieldName))
                "date_histogram"
              else
                "terms"

            val bucketsLabel =
              if (coordinatesField.map(_.name).contains(fieldName))
                "ranges"
              else if (dateFields.map(_.name).contains(fieldName))
                "entries"
              else
                "terms"

            val pathToBuckets: Seq[JsObject] =
              if (dateFields.map(_.name).contains(fieldName)) {
                readObjectArray(root, fieldName, fieldName, "buckets")
              } else {
                readObjectArray(root, fieldName, "buckets")
              }

            Facet(
              field = fieldName,
              `type` = `type`,
              buckets = pathToBuckets.map(_.toJson.convertTo[Bucket]),
              bucketsLabel = bucketsLabel
            )
          })

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
        aggObject = JsObject(
          aggObject.fields + (agg.field ->
            JsObject(
              "_type" -> agg.`type`.toJson,
              agg.bucketsLabel -> agg.buckets.toJson
            )
            )
        )
      })

      aggObject.toJson
    }
  }

  implicit object SingleDPLADocFormat extends RootJsonFormat[SingleDPLADoc] {

    def read(json: JsValue): SingleDPLADoc = {
      val root: JsObject = json.asJsObject

      SingleDPLADoc(
        docs = Seq(readObject(root, "_source").getOrElse(JsObject()))
      )
    }

    def write(singleDPLADoc: SingleDPLADoc): JsValue = {
      JsObject(
        "count" -> JsNumber(1),
        "docs" -> singleDPLADoc.docs.toJson
      ).toJson
    }
  }

  implicit object DPLADocListFormat extends RootJsonFormat[DPLADocList] {

    def read(json: JsValue): DPLADocList = {
      val root = json.asJsObject

      DPLADocList(
        count = readInt(root, "hits", "total", "value"),
        limit = None,
        start = None,
        docs = readObjectArray(root, "hits", "hits")
          .map { hit => readObject(hit, "_source").getOrElse(JsObject()).toJson },
        facets = readObject(root, "aggregations")
          .map(_.toJson.convertTo[FacetList])
      )
    }

    def write(dplaDocList: DPLADocList): JsValue = {
      val base: JsObject = JsObject(
        "count" -> dplaDocList.count.toJson,
        "start" -> dplaDocList.start.toJson,
        "limit" -> dplaDocList.limit.toJson,
        "docs" -> dplaDocList.docs.toJson
      )

      // Add facets.
      // If there are no facets, include an empty list (for compatability with
      // the legacy DPLA API)
      val complete: JsObject =
      if (dplaDocList.facets.nonEmpty)
        JsObject(base.fields + ("facets" -> dplaDocList.facets.toJson))
      else
        JsObject(base.fields + ("facets" -> JsArray()))

      complete.toJson
    }
  }

  /** Methods for writing JSON * */

  // Filter out fields whose values are:
  // null, empty array, or object with all empty fields.
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
