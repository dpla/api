package dpla.publications

import JsonFormats._
import spray.json._

object ElasticSearchQueryBuilder {

  def composeQuery(params: SearchParams): JsValue =
    JsObject(
      "from" -> params.from.toJson,
      "size" -> params.pageSize.toJson,
      "query" -> query(params.q, params.filters),
      "aggs" -> aggs(params.facets, params.facetSize)
    ).toJson

  // Map DPLA MAP fields to ElasticSearch fields
  private def dplaToElasticSearch(dplaField: String): String = {
    val fieldMap = Map(
      "dataProvider" -> "sourceUri",
      "isShownAt" -> "itemUri",
      "object" -> "payloadUri",
      "sourceResource.creator" -> "author",
      "sourceResource.date.displayDate" -> "publicationDate",
      "sourceResource.description" -> "summary",
      "sourceResource.format" -> "medium",
      "sourceResource.language.name" -> "language",
      "sourceResource.publisher" -> "publisher",
      "sourceResource.subject.name" -> "genre",
      "sourceResource.subtitle" -> "subtitle",
      "sourceResource.title" -> "title"
    )
    fieldMap(dplaField)
  }

  private def query(q: Option[String], fieldFilters: Seq[FieldFilter]) = {
    val keyword: Seq[JsObject] = q.map(keywordQuery).toSeq
    val filters: Seq[JsObject] = fieldFilters.map(singleFieldFilter)
    val mustTerms: Seq[JsObject] = keyword ++ filters

    if (mustTerms.isEmpty)
      JsObject(
        "match_all" -> JsObject()
      )
    else
      JsObject(
        "bool" -> JsObject(
          "must" -> mustTerms.toJson
        )
      )
  }

  private def keywordQuery(q: String): JsObject =
    JsObject(
      "query_string" -> JsObject(
        "fields" -> keywordQueryFields,
        "query" -> q.toJson,
        "analyze_wildcard" -> true.toJson,
        "default_operator" -> "AND".toJson,
        "lenient" -> true.toJson
      )
    )

  private def singleFieldFilter(filter: FieldFilter): JsObject =
    JsObject(
      "match" -> JsObject(
        filter.fieldName -> filter.value.toJson
      )
    )

//  private def keywordQuery(q: Option[String]): JsObject =
//    q match {
//      case Some(keyword) =>
//        JsObject(
//          "query_string" -> JsObject(
//            "fields" -> keywordQueryFields,
//            "query" -> keyword.toJson,
//            "analyze_wildcard" -> true.toJson,
//            "default_operator" -> "AND".toJson,
//            "lenient" -> true.toJson
//          )
//        )
//      case None =>
//        JsObject(
//          "match_all" -> JsObject()
//        )
//    }

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

  // Composes an aggregates (facets) query object
  private def aggs(facets: Option[Seq[String]], facetSize: Int): JsObject =
    facets match {
      case Some(facetArray) =>
        var base = JsObject()
        // Iterate through each facet and add a field to the base JsObject
        facetArray.foreach(facet => {
          val terms = JsObject(
            "terms" -> JsObject(
              "field" -> dplaToElasticSearch(facet).toJson,
              "size" -> facetSize.toJson
            )
          )
          base = JsObject(base.fields + (facet -> terms))
        })
        base
      case None => JsObject()
    }
}
