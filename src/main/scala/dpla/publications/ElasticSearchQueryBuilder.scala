package dpla.publications

import JsonFormats._
import spray.json._

object ElasticSearchQueryBuilder {

  def composeQuery(params: SearchParams): JsValue =
    JsObject(
      "from" -> params.from.toJson,
      "size" -> params.pageSize.toJson,
      "query" -> keywordQuery(params.q)
    ).toJson

  private def keywordQuery(q: Option[String]): JsObject =
    q match {
      case Some(keyword) =>
        JsObject(
        "query_string" -> JsObject(
            "fields" -> keywordQueryFields,
            "query" -> keyword.toJson,
            "analyze_wildcard" -> true.toJson,
            "default_operator" -> "AND".toJson,
            "lenient" -> true.toJson
          )
        )
      case None =>
        JsObject(
          "match_all" -> JsObject()
        )
    }

  // Fields to search in a keyword query and their boost values
  private val keywordQueryFields: JsArray = JsArray(
    "author^1".toJson,
    "genre^1".toJson,
    "medium^1".toJson,
    "language^1".toJson,
    "publisher^1".toJson,
    "subtitle^2".toJson,
    "summary^0.75".toJson,
    "title^2".toJson
  )
}
