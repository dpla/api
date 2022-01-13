package dpla.publications

import JsonFormats._
import spray.json._

object ElasticSearchQueryBuilder {

  def composeQuery(params: SearchParams): JsValue =
    JsObject(
      "from" -> params.from.toJson,
      "size" -> params.pageSize.toJson,
      "query" -> query(params.q, params.filters, params.exactFieldMatch),
      "aggs" -> aggs(params.facets, params.facetSize)
    ).toJson

  /**
   *  Map DPLA MAP fields to ElasticSearch fields
   *  Fields are either analyzed (text) or not (keyword) as is their default in the index
   */
  private def elasticSearchField(dplaField: String): String = {
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

  /**
   * Map DPLA MAP fields to ElasticSearch non-analyzed fields.
   * If a field is only indexed as analyzed (text), then return the analyzed field.
   * Used for exact field matches and facets.
   */
  private def exactMatchElasticSearchField(dplaField: String): String = {
    val fieldMap = Map(
      "dataProvider" -> "sourceUri",
      "isShownAt" -> "itemUri",
      "object" -> "payloadUri",
      "sourceResource.creator" -> "author.not_analyzed",
      "sourceResource.date.displayDate" -> "publicationDate.not_analyzed",
      "sourceResource.description" -> "summary",
      "sourceResource.format" -> "medium.not_analyzed",
      "sourceResource.language.name" -> "language.not_analyzed",
      "sourceResource.publisher" -> "publisher.not_analyzed",
      "sourceResource.subject.name" -> "genre.not_analyzed",
      "sourceResource.subtitle" -> "subtitle.not_analyzed",
      "sourceResource.title" -> "title.not_analyzed"
    )
    fieldMap(dplaField)
  }

  private def query(q: Option[String], fieldFilters: Seq[FieldFilter], exactFieldMatch: Boolean) = {
    val keyword: Seq[JsObject] = q.map(keywordQuery).toSeq
    val filters: Seq[JsObject] = fieldFilters.map(singleFieldFilter(_, exactFieldMatch))
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

  private def singleFieldFilter(filter: FieldFilter, exactFieldMatch: Boolean): JsObject = {
    // "term" searches for an exact term (with no additional text before or after).
    //   It is case-sensitive and does not analyze the search term.
    //   You can optionally set a parameter to ignore case, but this is NOT applied in the cultural heritage API.
    // "match" does a keyword search within the field.
    //   It is case-insensitive and analyzes the search term.
    val queryType: String = if (exactFieldMatch) "term" else "match"

    val field: String =
      if (exactFieldMatch) exactMatchElasticSearchField(filter.fieldName)
      else elasticSearchField(filter.fieldName)

    JsObject(
      queryType -> JsObject(
         field -> filter.value.toJson
      )
    )
  }

  // Composes an aggregates (facets) query object
  private def aggs(facets: Option[Seq[String]], facetSize: Int): JsObject =
    facets match {
      case Some(facetArray) =>
        var base = JsObject()
        // Iterate through each facet and add a field to the base JsObject
        facetArray.foreach(facet => {
          val terms = JsObject(
            "terms" -> JsObject(
              "field" -> exactMatchElasticSearchField(facet).toJson,
              "size" -> facetSize.toJson
            )
          )
          base = JsObject(base.fields + (facet -> terms))
        })
        base
      case None => JsObject()
    }
}
