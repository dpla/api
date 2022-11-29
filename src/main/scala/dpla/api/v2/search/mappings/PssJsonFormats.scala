//package dpla.api.v2.search.mappings
//
//import dpla.api.v2.search.models.PssFields
//import spray.json.{DefaultJsonProtocol, JsArray, JsNull, JsNumber, JsObject, JsValue, RootJsonFormat}
//
///**
// * These formats are used for parsing ElasticSearch responses and mapping them
// * to Primary Source Sets output.
// */
//object PssJsonFormats extends DefaultJsonProtocol with JsonFieldReader
//  with PssFields {
//
//  implicit object SingleDPLADocFormat extends RootJsonFormat[SingleDPLADoc] {
//
//    def read(json: JsValue): SingleDPLADoc = {
//      val root: JsObject = json.asJsObject
//
//      SingleDPLADoc(
//
//        docs = Seq(readObject(root, "_source").getOrElse(JsObject()))
//      )
//    }
//
//    def write(singleDPLADoc: SingleDPLADoc): JsValue = {
//      JsObject(
//        "count" -> JsNumber(1),
//        "docs" -> singleDPLADoc.docs.toJson
//      ).toJson
//    }
//  }
//
//  implicit object DPLADocListFormat extends RootJsonFormat[DPLADocList] {
//
//    def read(json: JsValue): DPLADocList = {
//      val root = json.asJsObject
//
//      DPLADocList(
//        count = readInt(root, "hits", "total", "value"),
//        limit = None,
//        start = None,
//        docs = readObjectArray(root, "hits", "hits")
//          .map { hit => readObject(hit, "_source").getOrElse(JsObject()).toJson },
//        facets = readObject(root, "aggregations")
//          .map(_.toJson.convertTo[FacetList])
//      )
//    }
//
//    def write(dplaDocList: DPLADocList): JsValue = {
//      val base: JsObject = JsObject(
//        "count" -> dplaDocList.count.toJson,
//        "start" -> dplaDocList.start.toJson,
//        "limit" -> dplaDocList.limit.toJson,
//        "docs" -> dplaDocList.docs.toJson
//      )
//
//      // Add facets.
//      // If there are no facets, include an empty list (for compatability with
//      // the legacy DPLA API)
//      val complete: JsObject =
//      if (dplaDocList.facets.nonEmpty)
//        JsObject(base.fields + ("facets" -> dplaDocList.facets.toJson))
//      else
//        JsObject(base.fields + ("facets" -> JsArray()))
//
//      complete.toJson
//    }
//  }
//
//  /** Methods for writing JSON * */
//
//  // Filter out fields whose values are:
//  // null, empty array, or object with all empty fields.
//  def filterIfEmpty(obj: JsObject): JsObject = {
//
//    def filterFields(fields: Map[String, JsValue]): Map[String, JsValue] =
//      fields.filterNot(_._2 match {
//        case JsNull => true
//        case JsArray(values) =>
//          if (values.isEmpty) true else false
//        case JsObject(fields) =>
//          if (filterFields(fields).isEmpty) true else false
//        case _ => false
//      })
//
//    val filtered = filterFields(obj.fields)
//    JsObject(filtered)
//  }
//}
