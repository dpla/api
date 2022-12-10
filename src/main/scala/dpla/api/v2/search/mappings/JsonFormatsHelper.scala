package dpla.api.v2.search.mappings

import spray.json.{JsArray, JsNull, JsObject, JsValue}

object JsonFormatsHelper {

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
