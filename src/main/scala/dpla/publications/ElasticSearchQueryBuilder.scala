package dpla.publications

import JsonFormats._
import spray.json._

object ElasticSearchQueryBuilder {

  def composeQuery(params: SearchParams): JsValue =
    JsObject(
      "from" -> params.from.toJson,
      "size" -> params.pageSize.toJson,
      "query" -> keywordQuery(params.q),
      "aggs" -> aggQuery(params.facets, params.facetSize)
    ).toJson

  // Map DPLA MAP fields to ElasticSearch fields
  private def dplaToElasticSearch(dplaField: String): String = {
    val fieldMap = Map(
      "dataProvider" -> "sourceUri",
      "sourceResource.creator" -> "author",
      "sourceResource.format" -> "medium",
      "sourceResource.language.name" -> "language",
      "sourceResource.publisher" -> "publisher",
      "sourceResource.subject.name" -> "genre"
    )
    fieldMap(dplaField)
  }

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

  // Composes an aggregate (facet) query object
  private def aggQuery(facets: Option[Seq[String]], facetSize: Int): JsObject =
    facets match {
      case Some(facetArray) =>
        var base = JsObject()
        facetArray.foreach(facet =>
          base = JsObject(base.fields + (facet -> singleAgg(facet, facetSize)))
        )
        base
      case None => JsObject()
    }

  private def singleAgg(facet: String, facetSize: Int): JsObject = {
    JsObject(
      "terms" -> JsObject(
        "field" -> dplaToElasticSearch(facet).toJson,
        "size" -> facetSize.toJson
      )
    )
  }
}
