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

  // Fields to search in a keyword query and their boost values
  private val keywordQueryFields= Seq(
    "author^1",
    "genre^1",
    "medium^1",
    "language^1",
    "publisher^1",
    "subtitle^2",
    "summary^0.75",
    "title^2"
  )

  private def query(q: Option[String], fieldFilters: Seq[FieldFilter], exactFieldMatch: Boolean) = {
    val keyword: Seq[JsObject] = q.map(keywordQuery(_, keywordQueryFields)).toSeq
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

  /**
   * A general keyword query on the given fields.
   *   "query_string" does a keyword search within the given fields.
   *   It is case-insensitive and analyzes the search term.
   */
  private def keywordQuery(q: String, fields: Seq[String]): JsObject =
    JsObject(
      "query_string" -> JsObject(
        "fields" -> fields.toJson,
        "query" -> q.toJson,
        "analyze_wildcard" -> true.toJson,
        "default_operator" -> "AND".toJson,
        "lenient" -> true.toJson
      )
    )

  /**
   * For general field filter, use a keyword (i.e. "query_string") query.
   * For exact field match, use "term" query.
   *   "term" searches for an exact term (with no additional text before or after).
   *   It is case-sensitive and does not analyze the search term.
   *   You can optionally set a parameter to ignore case, but this is NOT applied in the cultural heritage API.
   *   It is only for fields that non-analyzed (i.e. indexed as "keyword")
   */
  private def singleFieldFilter(filter: FieldFilter, exactFieldMatch: Boolean): JsObject = {
    if (exactFieldMatch)
      JsObject(
        "term" -> JsObject(
          exactMatchElasticSearchField(filter.fieldName) -> filter.value.toJson
        )
      )
    else {
      val fields: Seq[String] = Seq(elasticSearchField(filter.fieldName))
      keywordQuery(filter.value, fields)
    }
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
