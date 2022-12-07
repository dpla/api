package dpla.api.v2.search.queryBuilders

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import dpla.api.v2.search.SearchProtocol._
import dpla.api.v2.search.models.DPLAMAPFields
import dpla.api.v2.search.paramValidators._
import spray.json._
import dpla.api.v2.search.mappings.DPLAMAPJsonFormats._

import scala.collection.mutable.ArrayBuffer

/**
 * Composes ElasticSearch queries from user-submitted parameters.
 */
object QueryBuilder extends DPLAMAPFields {

  def apply(nextPhase: ActorRef[IntermediateSearchResult]): Behavior[IntermediateSearchResult] = {

    Behaviors.receiveMessage[IntermediateSearchResult] {

      case ValidSearchParams(searchParams, replyTo) =>
        nextPhase ! SearchQuery(searchParams,
          composeSearchQuery(searchParams), replyTo)
        Behaviors.same

      case ValidFetchParams(ids, params, replyTo) =>
        if (ids.size == 1) {
          nextPhase ! FetchQuery(ids.head, params, None, replyTo)
        }
        else {
          nextPhase ! MultiFetchQuery(composeMultiFetchQuery(ids), replyTo)
        }
        Behaviors.same

      case ValidRandomParams(randomParams, replyTo) =>
        nextPhase ! RandomQuery(randomParams, composeRandomQuery(randomParams),
          replyTo)
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }

  def composeMultiFetchQuery(ids: Seq[String]): JsValue =
    JsObject(
      "from" -> 0.toJson,
      "size" -> ids.size.toJson,
      "query" -> JsObject(
        "terms" -> JsObject(
          "id" -> ids.toJson
        )
      ),
      "sort" -> JsObject(
        "id" -> JsObject(
          "order" -> "asc".toJson
        )
      )
    ).toJson

  def composeRandomQuery(params: RandomParams): JsValue = {
    val filterClause: Option[JsObject] = params.filter.map(filterQuery)

    // Setting "boost_mode" to "sum" ensures that if a filter is used, the
    // random query will return a different doc every time (otherwise, it will
    // return the same doc over and over).
    var functionScore = JsObject(
      "random_score" -> JsObject(),
      "boost_mode" -> "sum".toJson
    )

    if (filterClause.nonEmpty) {
      val boolQuery = JsObject(
        "bool" -> JsObject(
          "filter" -> filterClause.get
        )
      )

      functionScore = JsObject(functionScore.fields + ("query" -> boolQuery))
    }

    JsObject(
      "query" -> JsObject(
        "function_score" -> functionScore
      ),
      "size" -> 1.toJson,
    ).toJson
  }

  def composeSearchQuery(params: SearchParams): JsValue =
    JsObject(
      "from" -> from(params.page, params.pageSize).toJson,
      "size" -> params.pageSize.toJson,
      "query" -> query(params.q, params.filter, params.fieldQueries, params.exactFieldMatch, params.op),
      "aggs" -> aggs(params.facets, params.facetSize),
      "sort" -> sort(params),
      "_source" -> fieldRetrieval(params.fields),
      "track_total_hits" -> true.toJson
    ).toJson

  // Fields to search in a keyword query and their boost values
  private val keywordQueryFields = Seq(
    "dataProvider.name^1",
    "intermediateProvider^1",
    "provider.name^1",
    "sourceResource.collection.description^1",
    "sourceResource.collection.title^1",
    "sourceResource.contributor^1",
    "sourceResource.creator^1",
    "sourceResource.description^0.75",
    "sourceResource.extent^1",
    "sourceResource.format^1",
    "sourceResource.language.name^1",
    "sourceResource.publisher^1",
    "sourceResource.relation^1",
    "sourceResource.rights^1",
    "sourceResource.spatial.country^0.75",
    "sourceResource.spatial.county^1",
    "sourceResource.spatial.name^1",
    "sourceResource.spatial.region^1",
    "sourceResource.spatial.state^0.75",
    "sourceResource.specType^1",
    "sourceResource.subject.name^1",
    "sourceResource.subtitle^2",
    "sourceResource.title^2",
    "sourceResource.type^1"
  )

  // ElasticSearch param that defines the number of hits to skip
  private def from(page: Int, pageSize: Int): Int = (page - 1) * pageSize

  private def query(q: Option[String],
                    filter: Option[Filter],
                    fieldQueries: Seq[FieldQuery],
                    exactFieldMatch: Boolean,
                    op: String
                   ) = {

    val keyword: Seq[JsObject] =
      q.map(keywordQuery(_, keywordQueryFields)).toSeq
    val filterClause: Option[JsObject] = filter.map(filterQuery)
    val fieldQuery: Seq[JsObject] =
      fieldQueries.flatMap(singleFieldQuery(_, exactFieldMatch))
    val queryTerms: Seq[JsObject] = keyword ++ fieldQuery
    val boolTerm: String = if (op == "OR") "should" else "must"

    if (queryTerms.isEmpty && filterClause.isEmpty)
      JsObject(
        "match_all" -> JsObject()
      )
    else {
      var boolBase = JsObject()

      if (queryTerms.nonEmpty) {
        boolBase = JsObject(boolBase.fields + (boolTerm -> queryTerms.toJson))
      }

      if (filterClause.nonEmpty) {
        boolBase = JsObject(boolBase.fields + ("filter" -> filterClause.get))
      }

      JsObject("bool" -> boolBase)
    }
  }

  /**
   * A general keyword query on the given fields.
   * "query_string" does a keyword search within the given fields.
   * It is case-insensitive and analyzes the search term.
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
   * A filter for a specific field.
   * This will filter out fields that do not match the given value, but will
   * not affect the score for matching documents.
   */
  private def filterQuery(filter: Filter): JsObject =
    JsObject(
      "bool" -> JsObject(
        "must" -> JsObject(
          "term" -> JsObject(
            filter.fieldName -> filter.value.toJson
          )
        )
      )
    )

  /**
   * For general field query, use a keyword (i.e. "query_string") query.
   * For exact field match, use "term" query.
   * - term" searches for an exact term (with no additional text before or after).
   * - It is case-sensitive and does not analyze the search term.
   * - You can optionally set a parameter to ignore case,
   * - but this is NOT applied in the cultural heritage API.
   * - It is only for fields that non-analyzed (i.e. indexed as "keyword")
   */
  private def singleFieldQuery(fieldQuery: FieldQuery,
                               exactFieldMatch: Boolean): Seq[JsObject] =

    if (fieldQuery.fieldName.endsWith(".before")) {
      // Range query
      val field: String = getElasticSearchField(fieldQuery.fieldName)
        .getOrElse(
          throw new RuntimeException("Unrecognized field name: " + fieldQuery.fieldName)
        )

      val obj = JsObject(
        "range" -> JsObject(
          field -> JsObject(
            "lte" -> fieldQuery.value.toJson
          )
        )
      )
      Seq(obj)

    } else if (fieldQuery.fieldName.endsWith(".after")) {
      // Range query
      val field: String = getElasticSearchField(fieldQuery.fieldName)
        .getOrElse(
          throw new RuntimeException("Unrecognized field name: " + fieldQuery.fieldName)
        )

      val obj = JsObject(
        "range" -> JsObject(
          field -> JsObject(
            "gte" -> fieldQuery.value.toJson
          )
        )
      )
      Seq(obj)

    } else if (exactFieldMatch) {
      // Exact match query
      val field: String = getElasticSearchExactMatchField(fieldQuery.fieldName)
        .getOrElse(
          throw new RuntimeException("Unrecognized field name: " + fieldQuery.fieldName)
        ) // This should not happen

      val values = stripLeadingAndTrainingQuotationMarks(fieldQuery.value)
        .split("AND")
        .flatMap(_.split("OR"))
        .map(_.trim)
        .map(stripLeadingAndTrainingQuotationMarks)

      values.map { value =>
        JsObject(
          "term" -> JsObject(
            field -> value.toJson
          )
        )
      }

    } else {
      // Basic field query
      val fields: Seq[String] =
        Seq(getElasticSearchField(fieldQuery.fieldName)).flatten
      val obj: JsObject = keywordQuery(fieldQuery.value, fields)
      Seq(obj)
    }

  /**
   * Strip leading and trailing quotation marks only if there are no
   * internal quotation marks.
   */
  private def stripLeadingAndTrainingQuotationMarks(str: String): String =
    if (str.matches("^\"[^\"]*\"$"))
      str.stripPrefix("\"").stripSuffix("\"")
    else
      str

  /**
   * Composes an aggregates (facets) query object.
   * Fields must be non-analyzed (i.e. indexed as keyword)
   */
  private def aggs(facets: Option[Seq[String]], facetSize: Int): JsObject =
    facets match {
      case Some(facetArray) =>
        var base = JsObject()
        // Iterate through each facet and add a field to the base JsObject
        facetArray.foreach(facet => {
          if (coordinatesField.map(_.name).contains(facet.split(":").head)) {
            // Spatial facet
            coordinatesField.map(_.elasticSearchDefault) match {
              case Some(field) =>
                val cleanFacetName = facet.split(":").head
                val coordinates = facet.split(":").drop(1).mkString(",")
                val ranges = ArrayBuffer.empty[JsValue]

                for (i <- 0 to 2000 by 100)
                  ranges += JsObject("from" -> i.toJson, "to" -> (i + 99).toJson)
                ranges += JsObject("from" -> 2100.toJson)

                val geoDistance = JsObject(
                  "geo_distance" -> JsObject(
                    "field" -> field.toJson,
                    "origin" -> coordinates.toJson,
                    "unit" -> "mi".toJson,
                    "ranges" -> ranges.toArray.toJson
                  )
                )
                base = JsObject(base.fields + (cleanFacetName -> geoDistance))

              case None => base
            }
          } else if (dateFields.map(_.name).contains(facet)) {
            // Dates facet
            val esField = getElasticSearchField(facet).getOrElse(
              throw new RuntimeException("Unrecognized facet name: " + facet)
            )

            val interval = facet.split("\\.").lastOption match {
              case Some("month") => "month"
              case _ => "year"
            }

            val format = facet.split("\\.").lastOption match {
              case Some("month") => "yyyy-MM"
              case _ => "yyyy"
            }

            val gte = facet.split("\\.").lastOption match {
              case Some("month") => "now-416y"
              case _ => "now-2000y"
            }

            val dateHistogram = JsObject(
              "filter" -> JsObject(
                "range" -> JsObject(
                  esField -> JsObject(
                    "gte" -> gte.toJson,
                    "lte" -> "now".toJson
                  )
                )
              ),
              "aggs" -> JsObject(
                facet -> JsObject(
                  "date_histogram" -> JsObject(
                    "field" -> esField.toJson,
                    "interval" -> interval.toJson,
                    "format" -> format.toJson,
                    "min_doc_count" -> 1.toJson,
                    "order" -> JsObject(
                      "_key" -> "desc".toJson
                    )
                  )
                )
              )
            )

            base = JsObject(base.fields + (facet -> dateHistogram))

          } else {
            // Regular facet
            val terms = JsObject(
              "terms" -> JsObject(
                "field" -> getElasticSearchExactMatchField(facet).toJson,
                "size" -> facetSize.toJson
              )
            )
            base = JsObject(base.fields + (facet -> terms))
          }
        })
        base
      case None => JsObject()
    }

  private def sort(params: SearchParams): JsValue = {

    val defaultSort: JsArray =
      JsArray(
        "_score".toJson,
        "_doc".toJson
      )

    // This is the fastest way to sort documents but is meaningless.
    // It is the order in which they are saved to disk.
    val diskSort: JsArray =
    JsArray(
      "_doc".toJson
    )

    params.sortBy match {
      case Some(field) =>
        if (coordinatesField.map(_.name).contains(field)) {
          // Geo sort
          coordinatesField.map(_.elasticSearchDefault) match {
            case Some(coordinates) =>
              params.sortByPin match {
                case Some(pin) =>
                  JsArray(
                    JsObject(
                      "_geo_distance" -> JsObject(
                        coordinates -> pin.toJson,
                        "order" -> "asc".toJson,
                        "unit" -> "mi".toJson
                      )
                    ),
                    "_score".toJson,
                    "_doc".toJson
                  )
                // No sort_by_pin parameter
                case None => defaultSort
              }
            // No ElasticSearch mapping for this field
            case None => defaultSort
          }
        } else {
          // Regular sort
          getElasticSearchNotAnalyzed(field) match {
            case Some(esField) =>
              JsArray(
                JsObject(
                  esField -> JsObject(
                    "order" -> params.sortOrder.toJson
                  )
                ),
                "_score".toJson,
                "_doc".toJson
              )
            // No ElasticSearch mapping for this field
            case None => defaultSort
          }
        }
      // No sort_by parameter
      case None =>
        if (params.q.isEmpty && params.fieldQueries.isEmpty)
          diskSort
        else
          defaultSort
    }
  }

  private def fieldRetrieval(fields: Option[Seq[String]]): JsValue = {
    fields match {
      case Some(f) => f.map(getElasticSearchField).toJson
      case None => Seq("*").toJson
    }
  }
}
