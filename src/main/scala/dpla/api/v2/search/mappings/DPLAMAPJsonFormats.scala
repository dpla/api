package dpla.api.v2.search.mappings

import dpla.api.v2.search.mappings.JsonFormatsHelper._
import dpla.api.v2.search.models.DPLAMAPFields
import org.slf4j.LoggerFactory
import spray.json._

/**
 * These formats are used for parsing ElasticSearch responses and mapping them
 * to DPLA MAP.
 */
object DPLAMAPJsonFormats extends JsonFieldReader
  with DPLAMAPFields {

  private val log = LoggerFactory.getLogger(getClass)

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

    def write(bucket: Bucket): JsValue =
      filterIfEmpty(JsObject(
        "term" -> bucket.key.toJson,
        "time" -> bucket.keyAsString.toJson,
        "count" -> bucket.docCount.toJson,
        "to" -> bucket.to.toJson,
        "from" -> bucket.from.toJson
      )).toJson
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

            // Warn when ES truncated a terms aggregation (more buckets exist than returned)
            if (`type` == "terms" && log.isWarnEnabled) {
              readInt(root, fieldName, "sum_other_doc_count").foreach { n =>
                if (n > 0)
                  log.warn(s"ES terms aggregation '$fieldName' truncated: $n additional docs not in response")
              }
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

      // Warn on partial results — partial results are silently returned otherwise
      if (log.isWarnEnabled) {
        if (readBoolean(root, "timed_out").contains(true))
          log.warn("ES query timed out — results may be partial")
        readInt(root, "_shards", "failed").foreach { n =>
          if (n > 0) log.warn(s"ES query had $n failed shard(s) — results may be partial")
        }
      }

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
}
