package dpla.api.v2.search

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.api.v2.search.SearchProtocol.{FetchQuery, IntermediateSearchResult, MultiFetchQuery, RandomQuery, SearchQuery, SearchResponse, ValidFetchIds, ValidRandomParams, ValidSearchParams}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, PrivateMethodTester}
import spray.json._

class QueryBuilderTest extends AnyWordSpec with Matchers
  with PrivateMethodTester with JsonFieldReader with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  val interProbe: TestProbe[IntermediateSearchResult] =
    testKit.createTestProbe[IntermediateSearchResult]

  val replyProbe: TestProbe[SearchResponse] =
    testKit.createTestProbe[SearchResponse]

  val queryBuilder: ActorRef[IntermediateSearchResult] =
    testKit.spawn(QueryBuilder(interProbe.ref))

  def getJsSearchQuery(params: SearchParams): JsObject = {
    queryBuilder ! ValidSearchParams(params, replyProbe.ref)
    val msg: SearchQuery = interProbe.expectMessageType[SearchQuery]
    msg.query.asJsObject
  }

  def getJsFetchQuery(ids: Seq[String]): JsObject = {
    queryBuilder ! ValidFetchIds(ids, replyProbe.ref)
    val msg: MultiFetchQuery = interProbe.expectMessageType[MultiFetchQuery]
    msg.query.asJsObject
  }

  def getJsRandomQuery(params: RandomParams): JsObject = {
    queryBuilder ! ValidRandomParams(params, replyProbe.ref)
    val msg: RandomQuery = interProbe.expectMessageType[RandomQuery]
    msg.query.asJsObject
  }

  val minSearchParams: SearchParams = SearchParams(
    exactFieldMatch = false,
    facets = None,
    facetSize = 100,
    fields = None,
    fieldQueries = Seq[FieldQuery](),
    filter = None,
    op = "AND",
    page = 3,
    pageSize = 20,
    q = None,
    sortBy = None,
    sortByPin = None,
    sortOrder = "asc"
  )

  val minQuery: JsObject = getJsSearchQuery(minSearchParams)

  val detailSearchParams: SearchParams = SearchParams(
    exactFieldMatch = false,
    facets = Some(
      Seq(
        "provider.@id",
        "sourceResource.publisher",
        "sourceResource.subject.name"
      )
    ),
    facetSize = 100,
    fields = Some(Seq("sourceResource.title")),
    fieldQueries = Seq(FieldQuery("sourceResource.subject.name", "adventure")),
    filter = None,
    op = "AND",
    page = 3,
    pageSize = 20,
    q = Some("dogs"),
    sortBy = Some("sourceResource.title"),
    sortByPin = None,
    sortOrder = "desc"
  )

  val detailQuery: JsObject = getJsSearchQuery(detailSearchParams)

  val geoSortSearchParams: SearchParams = SearchParams(
    exactFieldMatch = false,
    facets = None,
    facetSize = 100,
    fields = None,
    fieldQueries = Seq[FieldQuery](),
    filter = None,
    op = "AND",
    page = 3,
    pageSize = 20,
    q = None,
    sortBy = Some("sourceResource.spatial.coordinates"),
    sortByPin = Some("26.15952,-97.99084"),
    sortOrder = "asc"
  )

  val geoSortQuery: JsObject = getJsSearchQuery(geoSortSearchParams)

  val geoFacetSearchParams: SearchParams = SearchParams(
    exactFieldMatch = false,
    facets = Some(Seq("sourceResource.spatial.coordinates:42:-70")),
    facetSize = 100,
    fields = None,
    fieldQueries = Seq[FieldQuery](),
    filter = None,
    op = "AND",
    page = 3,
    pageSize = 20,
    q = None,
    sortBy = None,
    sortByPin = None,
    sortOrder = "asc"
  )

  val geoFacetQuery: JsObject = getJsSearchQuery(geoFacetSearchParams)

  val dateFacetSearchParams: SearchParams = SearchParams(
    exactFieldMatch = false,
    facets = Some(Seq("sourceResource.date.begin")),
    facetSize = 100,
    fields = None,
    fieldQueries = Seq[FieldQuery](),
    filter = None,
    op = "AND",
    page = 3,
    pageSize = 20,
    q = None,
    sortBy = None,
    sortByPin = None,
    sortOrder = "asc"
  )

  val dateFacetQuery: JsObject = getJsSearchQuery(dateFacetSearchParams)

  val filterParams: SearchParams = SearchParams(
    exactFieldMatch = false,
    facets = None,
    facetSize = 100,
    fields = None,
    fieldQueries = Seq[FieldQuery](),
    filter = Some(Filter("provider.@id", "http://dp.la/api/contributor/lc")),
    op = "AND",
    page = 3,
    pageSize = 20,
    q = None,
    sortBy = None,
    sortByPin = None,
    sortOrder = "asc"
  )

  val filterQuery: JsObject = getJsSearchQuery(filterParams)

  val multiFetchIds = Seq(
    "b70107e4fe29fe4a247ae46e118ce192",
    "17b0da7b05805d78daf8753a6641b3f5"
  )

  val multiFetchQuery: JsObject = getJsFetchQuery(multiFetchIds)

  val randomParams: RandomParams = RandomParams(
    filter = Some(Filter("provider.@id", "http://dp.la/api/contributor/lc")),
  )

  val randomQuery: JsObject = getJsRandomQuery(randomParams)

  val elasticSearchField: PrivateMethod[String] =
    PrivateMethod[String](Symbol("elasticSearchField"))

  val exactMatchElasticSearchField: PrivateMethod[String] =
    PrivateMethod[String](Symbol("exactMatchElasticSearchField"))

  "fetch query builder" should {
    "specify id" in {
      val id = "b70107e4fe29fe4a247ae46e118ce192"
      queryBuilder ! ValidFetchIds(Seq(id), replyProbe.ref)
      val msg = interProbe.expectMessageType[FetchQuery]
      assert(msg.id == id)
    }
  }

  "multi-fetch query builder" should {
    "specify from" in {
      val expected = Some(0)
      val traversed = readInt(multiFetchQuery, "from")
      assert(traversed == expected)
    }

    "specify size" in {
      val expected = Some(2)
      val traversed = readInt(multiFetchQuery, "size")
      assert(traversed == expected)
    }

    "specify ids" in {
      val traversed = readStringArray(multiFetchQuery, "query", "terms", "id")
      traversed should contain allElementsOf multiFetchIds
    }

    "specify sort order" in {
      val expected = Some("asc")
      val traversed = readString(multiFetchQuery, "sort", "id", "order")
      assert(traversed == expected)
    }
  }

  "random query builder" should {
    "specify random score" in {
      val traversed = readObject(randomQuery, "query", "function_score",
        "random_score")
      traversed should not be None
    }

    "specify boost mode" in {
      val expected = Some("sum")
      val traversed = readString(randomQuery, "query", "function_score",
        "boost_mode")
      assert(traversed == expected)
    }

    "specify page size" in {
      val expected = Some(1)
      val traversed = readInt(randomQuery, "size")
      assert(traversed == expected)
    }

    "specify filter" in {
      val expected = Some("http://dp.la/api/contributor/lc")
      val traversed = readString(randomQuery, "query", "function_score",
        "query", "bool", "filter", "bool", "must", "term", "provider.@id")
      assert(traversed == expected)
    }
  }

  "search query builder" should {
    "specify from" in {
      val expected = Some(40)
      val traversed = readInt(minQuery, "from")
      assert(traversed == expected)
    }

    "specify size" in {
      val expected = Some(20)
      val traversed = readInt(minQuery, "size")
      assert(traversed == expected)
    }

    "specify track_total_hits" in {
      val expected = Some(true)
      val traversed = readBoolean(minQuery, "track_total_hits")
      assert(traversed == expected)
    }
  }

  "keyword query builder" should {
    "handle missing q (no field search)" in {
      val expected = Some(JsObject())
      val traversed = readObject(minQuery, "query", "match_all")
      assert(traversed == expected)
    }

    "specify keyword" in {
      val expected = Some("dogs")
      val boolMust = readObjectArray(detailQuery, "query", "bool", "must")
      val queryString =
        boolMust.flatMap(obj => readObject(obj, "query_string")).head
      val traversed = readString(queryString, "query")
      assert(traversed == expected)
    }

    "specify fields to search" in {
      val boolMust = readObjectArray(detailQuery, "query", "bool", "must")
      val queryString =
        boolMust.flatMap(obj => readObject(obj, "query_string")).head
      val traversed = readStringArray(queryString, "fields")
      assert(traversed.nonEmpty)
    }

    "specify wildcard analyzer" in {
      val expected = Some(true)
      val boolMust = readObjectArray(detailQuery, "query", "bool", "must")
      val queryString =
        boolMust.flatMap(obj => readObject(obj, "query_string")).head
      val traversed = readBoolean(queryString, "analyze_wildcard")
      assert(traversed == expected)
    }

    "specify default operator" in {
      val expected = Some("AND")
      val boolMust = readObjectArray(detailQuery, "query", "bool", "must")
      val queryString =
        boolMust.flatMap(obj => readObject(obj, "query_string")).head
      val traversed = readString(queryString, "default_operator")
      assert(traversed == expected)
    }

    "specify lenient" in {
      val expected = Some(true)
      val boolMust = readObjectArray(detailQuery, "query", "bool", "must")
      val queryString =
        boolMust.flatMap(obj => readObject(obj, "query_string")).head
      val traversed = readBoolean(queryString, "lenient")
      assert(traversed == expected)
    }
  }

  "field query builder" should {
    "handle no field search with q" in {
      val params = minSearchParams.copy(q=Some("dogs"))
      val query = getJsSearchQuery(params)
      val boolMust = readObjectArray(query, "query", "bool", "must")
      val queryString =
        boolMust.flatMap(obj => readObject(obj, "query_string"))
      assert(queryString.size == 1)
    }

    "handle field search with no q" in {
      val fieldQueries = Seq(FieldQuery("sourceResource.subject.name", "london"))
      val params = minSearchParams.copy(fieldQueries=fieldQueries)
      val query = getJsSearchQuery(params)
      val boolMust = readObjectArray(query, "query", "bool", "must")
      val queryString =

        boolMust.flatMap(obj => readObject(obj, "query_string"))
      assert(queryString.size == 1)
    }

    "handle multiple field searches" in {
      val fieldQueries = Seq(
        FieldQuery("sourceResource.subject.name", "london"),
        FieldQuery("provider.@id", "http://standardebooks.org")
      )
      val params = minSearchParams.copy(fieldQueries=fieldQueries)
      val query = getJsSearchQuery(params)
      val boolMust = readObjectArray(query, "query", "bool", "must")
      val queryMatch =
        boolMust.flatMap(obj => readObject(obj, "query_string"))
      assert(queryMatch.size == 2)
    }

    "specify field query term" in {
      val expected = Some("london")
      val fieldQuery = Seq(FieldQuery("sourceResource.subject.name", "london"))
      val params = minSearchParams.copy(fieldQueries=fieldQuery)
      val query = getJsSearchQuery(params)
      val boolMust = readObjectArray(query, "query", "bool", "must")
      val queryString =
        boolMust.flatMap(obj => readObject(obj, "query_string")).head
      val traversed = readString(queryString, "query")
      assert(traversed == expected)
    }

    "specify field to search" in {
      val expected = Seq("sourceResource.subject.name")
      val fieldQueries = Seq(FieldQuery("sourceResource.subject.name", "london"))
      val params = minSearchParams.copy(fieldQueries=fieldQueries)
      val query = getJsSearchQuery(params)
      val boolMust = readObjectArray(query, "query", "bool", "must")
      val queryString =
        boolMust.flatMap(obj => readObject(obj, "query_string")).head
      val traversed = readStringArray(queryString, "fields")
      assert(traversed == expected)
    }

    "exact term match" should {

      "use 'term' query" in {
        val params = detailSearchParams.copy(exactFieldMatch = true)
        val query = getJsSearchQuery(params)
        val boolMust = readObjectArray(query, "query", "bool", "must")
        val queryTerm = boolMust.flatMap(obj => readObject(obj, "term"))
        assert(queryTerm.size == 1)
      }

      "specify exact field match field and term" in {
        val expected = Some("adventure")
        val params = detailSearchParams.copy(exactFieldMatch = true)
        val query = getJsSearchQuery(params)
        val boolMust = readObjectArray(query, "query", "bool", "must")
        val queryTerm =
          boolMust.flatMap(obj => readObject(obj, "term")).head
        val traversed = readString(queryTerm, "sourceResource.subject.name.not_analyzed")
        assert(traversed == expected)
      }

      "strip leading and trailing quotation marks from term" in {
        val expected = Some("Mystery fiction")
        val fieldQueries =
          Seq(FieldQuery("sourceResource.subject.name", "\"Mystery fiction\""))
        val params = minSearchParams.copy(fieldQueries=fieldQueries, exactFieldMatch=true)
        val query = getJsSearchQuery(params)
        val boolMust = readObjectArray(query, "query", "bool", "must")
        val queryTerm =
          boolMust.flatMap(obj => readObject(obj, "term")).head
        val traversed = readString(queryTerm, "sourceResource.subject.name.not_analyzed")
        assert(traversed == expected)
      }

      "handle multiple terms joined by +AND+ or +OR+" in {
        val expected = Seq("Legislators", "City Council")
        val fieldQueries = Seq(FieldQuery(
          "sourceResource.subject.name",
          "\"Legislators\"+AND+\"City Council\""
        ))
        val params = minSearchParams.copy(fieldQueries=fieldQueries, exactFieldMatch=true)
        val query = getJsSearchQuery(params)
        val boolMust = readObjectArray(query, "query", "bool", "must")
        val queryTerms = boolMust.flatMap(obj => readObject(obj, "term"))
        val traversed =
          queryTerms.map(readString(_, "sourceResource.subject.name.not_analyzed"))
        assert(traversed == expected)
      }
    }

    "range query" should {
      "specify after value" in {
        val expected = Some("1980")
        val fieldQueries = Seq(FieldQuery("sourceResource.date.after", "1980"))
        val params = minSearchParams.copy(fieldQueries=fieldQueries)
        val query = getJsSearchQuery(params)
        val boolMust = readObjectArray(query, "query", "bool", "must")
        val queryRange =
          boolMust.flatMap(obj => readObject(obj, "range")).head
        val traversed = readString(queryRange, "sourceResource.date.end", "gte")
        assert(traversed == expected)
      }

      "specify before value" in {
        val expected = Some("1980")
        val fieldQueries = Seq(FieldQuery("sourceResource.date.before", "1980"))
        val params = minSearchParams.copy(fieldQueries=fieldQueries)
        val query = getJsSearchQuery(params)
        val boolMust = readObjectArray(query, "query", "bool", "must")
        val queryRange =
          boolMust.flatMap(obj => readObject(obj, "range")).head
        val traversed = readString(queryRange, "sourceResource.date.begin", "lte")
        assert(traversed == expected)
      }
    }
  }

  "op query builder" should {
    "set must for AND" in {
      val expected = "must"
      val parent = readObject(detailQuery, "query", "bool")
      val fieldNames = parent.get.fields.keys
      fieldNames should contain only expected
    }

    "set should for OR" in  {
      val expected = "should"
      val params = detailSearchParams.copy(op="OR")
      val query = getJsSearchQuery(params)
      val parent = readObject(query, "query", "bool")
      val fieldNames = parent.get.fields.keys
      fieldNames should contain only expected
    }
  }

  "agg query builder" should {
    "handle missing facets" in {
      val parent = readObject(minQuery, "aggs")
      val fieldNames = parent.get.fields.keys
      assert(fieldNames.isEmpty)
    }

    "include all facets" in {
      val expected =
        Seq("provider.@id", "sourceResource.publisher", "sourceResource.subject.name")
      val parent = readObject(detailQuery, "aggs")
      val fieldNames = parent.get.fields.keys
      fieldNames should contain allElementsOf expected
    }

    "specify facet field" in {
      val expected = Some("sourceResource.subject.name.not_analyzed")
      val traversed =
        readString(detailQuery, "aggs", "sourceResource.subject.name", "terms", "field")
      assert(traversed == expected)
    }

    "specify facet size" in {
      val expected = Some(100)
      val traversed =
        readInt(detailQuery, "aggs", "sourceResource.subject.name", "terms", "size")
      assert(traversed == expected)
    }
  }

  "geo agg query builder" should {
    "specify facet field" in {
      val expected = Some("sourceResource.spatial.coordinates")
      val traversed = readString(geoFacetQuery, "aggs",
        "sourceResource.spatial.coordinates", "geo_distance", "field")
      assert(traversed == expected)
    }

    "specify origin coordinates" in {
      val expected = Some("42,-70")
      val traversed = readString(geoFacetQuery, "aggs",
        "sourceResource.spatial.coordinates", "geo_distance", "origin")
      assert(traversed == expected)
    }

    "specify unit" in {
      val expected = Some("mi")
      val traversed = readString(geoFacetQuery, "aggs",
        "sourceResource.spatial.coordinates", "geo_distance", "unit")
      assert(traversed == expected)
    }

    "specify range start" in {
      val expected = Some(0)
      val range = readObjectArray(geoFacetQuery, "aggs",
        "sourceResource.spatial.coordinates", "geo_distance", "ranges").head
      val traversed = readInt(range, "from")
      assert(traversed == expected)
    }

    "specify range end" in {
      val expected = Some(99)
      val range = readObjectArray(geoFacetQuery, "aggs",
        "sourceResource.spatial.coordinates", "geo_distance", "ranges").head
      val traversed = readInt(range, "to")
      assert(traversed == expected)
    }
  }

  "date agg query builder" should {
    "specify facet field" in {
      val expected = Some("sourceResource.date.begin")
      val traversed = readString(dateFacetQuery, "aggs",
        "sourceResource.date.begin", "aggs", "sourceResource.date.begin",
        "date_histogram", "field")
      assert(traversed == expected)
    }

    "specify default interval" in {
      val expected = Some("year")
      val traversed = readString(dateFacetQuery, "aggs",
        "sourceResource.date.begin", "aggs", "sourceResource.date.begin",
        "date_histogram", "interval")
      assert(traversed == expected)
    }

    "specify year interval" in {
      val expected = Some("year")
      val params = minSearchParams
        .copy(facets = Some(Seq("sourceResource.date.begin.year")))
      val query = getJsSearchQuery(params)
      val traversed = readString(query, "aggs",
        "sourceResource.date.begin.year", "aggs",
        "sourceResource.date.begin.year", "date_histogram", "interval")
      assert(traversed == expected)
    }

    "specify month interval" in {
      val expected = Some("month")
      val params = minSearchParams
        .copy(facets = Some(Seq("sourceResource.date.begin.month")))
      val query = getJsSearchQuery(params)
      val traversed = readString(query, "aggs",
        "sourceResource.date.begin.month", "aggs",
        "sourceResource.date.begin.month", "date_histogram", "interval")
      assert(traversed == expected)
    }

    "specify default format" in {
      val expected = Some("yyyy")
      val traversed = readString(dateFacetQuery, "aggs",
        "sourceResource.date.begin", "aggs", "sourceResource.date.begin",
        "date_histogram", "format")
      assert(traversed == expected)
    }

    "specify year format" in {
      val expected = Some("yyyy")
      val params = minSearchParams
        .copy(facets = Some(Seq("sourceResource.date.end.year")))
      val query = getJsSearchQuery(params)
      val traversed = readString(query, "aggs",
        "sourceResource.date.end.year", "aggs",
        "sourceResource.date.end.year", "date_histogram", "format")
      assert(traversed == expected)
    }

    "specify month format" in {
      val expected = Some("yyyy-MM")
      val params = minSearchParams
        .copy(facets = Some(Seq("sourceResource.date.end.month")))
      val query = getJsSearchQuery(params)
      val traversed = readString(query, "aggs",
        "sourceResource.date.end.month", "aggs",
        "sourceResource.date.end.month", "date_histogram", "format")
      assert(traversed == expected)
    }

    "specify min doc count" in {
      val expected = Some("1")
      val traversed = readString(dateFacetQuery, "aggs",
        "sourceResource.date.begin", "aggs", "sourceResource.date.begin",
        "date_histogram", "min_doc_count")
      assert(traversed == expected)
    }

    "specify order" in {
      val expected = Some("desc")
      val traversed = readString(dateFacetQuery, "aggs",
        "sourceResource.date.begin", "aggs", "sourceResource.date.begin",
        "date_histogram", "order", "_key")
      assert(traversed == expected)
    }

    "specify default filter gte" in {
      val expected = Some("now-2000y")
      val traversed = readString(dateFacetQuery, "aggs",
        "sourceResource.date.begin", "filter", "range",
        "sourceResource.date.begin", "gte")
      assert(traversed == expected)
    }

    "specify year gte" in {
      val expected = Some("now-2000y")
      val params = minSearchParams
        .copy(facets = Some(Seq("sourceResource.date.end.year")))
      val query = getJsSearchQuery(params)
      val traversed = readString(query, "aggs",
        "sourceResource.date.end.year", "filter", "range",
        "sourceResource.date.end", "gte")
      assert(traversed == expected)
    }

    "specify month gte" in {
      val expected = Some("now-416y")
      val params = minSearchParams
        .copy(facets = Some(Seq("sourceResource.date.end.month")))
      val query = getJsSearchQuery(params)
      val traversed = readString(query, "aggs",
        "sourceResource.date.end.month", "filter", "range",
        "sourceResource.date.end", "gte")
      assert(traversed == expected)
    }

    "specify filter lte" in {
      val expected = Some("now")
      val traversed = readString(dateFacetQuery, "aggs",
        "sourceResource.date.begin", "filter", "range",
        "sourceResource.date.begin", "lte")
      assert(traversed == expected)
    }
  }

  "sort query builder" should {
    "default to sorting by score" in {
      val expected = Some("desc")
      val sortArray = readObjectArray(minQuery, "sort")
      val score = sortArray.flatMap(obj => readObject(obj, "_score")).head
      val traversed = readString(score, "order")
      assert(traversed == expected)
    }

    "include score sorting when sorting by field" in {
      val expected = Some("desc")
      val sortArray = readObjectArray(detailQuery, "sort")
      val score = sortArray.flatMap(obj => readObject(obj, "_score")).head
      val traversed = readString(score, "order")
      assert(traversed == expected)
    }

    "specify sort field and order" in {
      val expected = Some("desc")
      val sortArray = readObjectArray(detailQuery, "sort")
      val score = sortArray
        .flatMap(obj => readObject(obj, "sourceResource.title.not_analyzed"))
        .head
      val traversed = readString(score, "order")
      assert(traversed == expected)
    }
  }

  "spatial sort" should {
    "specify coordinates" in {
      val expected = Some("26.15952,-97.99084")
      val sortArray = readObjectArray(geoSortQuery, "sort")
      val geo = sortArray
        .flatMap(obj => readObject(obj, "_geo_distance"))
        .head
      val traversed = readString(geo, "sourceResource.spatial.coordinates")
      assert(traversed == expected)
    }

    "include sort order" in {
      val expected = Some("asc")
      val sortArray = readObjectArray(geoSortQuery, "sort")
      val geo = sortArray
        .flatMap(obj => readObject(obj, "_geo_distance"))
        .head
      val traversed = readString(geo, "order")
      assert(traversed == expected)
    }

    "include unit" in {
      val expected = Some("mi")
      val sortArray = readObjectArray(geoSortQuery, "sort")
      val geo = sortArray
        .flatMap(obj => readObject(obj, "_geo_distance"))
        .head
      val traversed = readString(geo, "unit")
      assert(traversed == expected)
    }
  }

  "fields retrieval query builder" should {
    "retrieve all fields by default" in {
      val expected = "*"
      val traversed = readStringArray(minQuery, "_source")
      traversed should contain only expected
    }

    "specify fields to retrieve" in {
      val expected = "sourceResource.title"
      val traversed = readStringArray(detailQuery, "_source")
      traversed should contain only expected
    }
  }

  "filter query builder" should {
    "specify field and term" in {
      val expected = Some("http://dp.la/api/contributor/lc")
      val traversed = readString(filterQuery, "query", "bool", "filter",
        "bool", "must", "term", "provider.@id")
      assert(traversed == expected)
    }
  }
}
