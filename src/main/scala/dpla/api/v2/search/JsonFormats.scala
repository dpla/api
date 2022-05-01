package dpla.api.v2.search

import spray.json.{DefaultJsonProtocol, _}

/**
 * These formats are used for parsing ElasticSearch responses and mapping them
 * to DPLA MAP.
 */
object JsonFormats extends DefaultJsonProtocol with JsonFieldReader {

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

          val facets: Seq[Facet] = fieldNames.map(fieldName =>
            Facet(
              field = fieldName,
              buckets = readObjectArray(root, fieldName, "buckets")
                .map(_.toJson.convertTo[Bucket])
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
        aggObject = JsObject(
          aggObject.fields + (agg.field -> JsObject("terms" -> agg.buckets.toJson))
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
          .map{hit => readObject(hit, "_source").getOrElse(JsObject()).toJson},
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

      // Add facets if there are any
      val complete: JsObject =
        if (dplaDocList.facets.nonEmpty)
          JsObject(base.fields + ("facets" -> dplaDocList.facets.toJson))
        else
          base

      complete.toJson
    }
  }

  /** Methods for writing JSON **/

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
