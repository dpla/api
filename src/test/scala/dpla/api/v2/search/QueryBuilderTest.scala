package dpla.api.v2.search

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.api.v2.search
import dpla.api.v2.search.SearchProtocol.{FetchQuery, IntermediateSearchResult, MultiFetchQuery, SearchQuery, SearchResponse, ValidFetchIds, ValidSearchParams}
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

  val minSearchParams: SearchParams = SearchParams(
    exactFieldMatch = false,
    facets = None,
    facetSize = 100,
    fields = None,
    filters = Seq[FieldFilter](),
    op = "AND",
    page = 3,
    pageSize = 20,
    q = None,
    sortBy = None,
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
    filters = Seq(FieldFilter("sourceResource.subject.name", "adventure")),
    op = "AND",
    page = 3,
    pageSize = 20,
    q = Some("dogs"),
    sortBy = Some("sourceResource.title"),
    sortOrder = "desc"
  )

  val detailQuery: JsObject = getJsSearchQuery(detailSearchParams)

  val multiFetchIds = Seq(
    "b70107e4fe29fe4a247ae46e118ce192",
    "17b0da7b05805d78daf8753a6641b3f5"
  )

  val multiFetchQuery: JsObject = getJsFetchQuery(multiFetchIds)

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

  "field filter query builder" should {
    "handle no field search with q" in {
      val params = minSearchParams.copy(q=Some("dogs"))
      val query = getJsSearchQuery(params)
      val boolMust = readObjectArray(query, "query", "bool", "must")
      val queryString =
        boolMust.flatMap(obj => readObject(obj, "query_string"))
      assert(queryString.size == 1)
    }

    "handle field search with no q" in {
      val filters = Seq(FieldFilter("sourceResource.subject.name", "london"))
      val params = minSearchParams.copy(filters=filters)
      val query = getJsSearchQuery(params)
      val boolMust = readObjectArray(query, "query", "bool", "must")
      val queryString =

        boolMust.flatMap(obj => readObject(obj, "query_string"))
      assert(queryString.size == 1)
    }

    "handle multiple field searches" in {
      val filters = Seq(
        FieldFilter("sourceResource.subject.name", "london"),
        FieldFilter("provider.@id", "http://standardebooks.org")
      )
      val params = minSearchParams.copy(filters=filters)
      val query = getJsSearchQuery(params)
      val boolMust = readObjectArray(query, "query", "bool", "must")
      val queryMatch =
        boolMust.flatMap(obj => readObject(obj, "query_string"))
      assert(queryMatch.size == 2)
    }

    "specify filter term" in {
      val expected = Some("london")
      val filters = Seq(FieldFilter("sourceResource.subject.name", "london"))
      val params = minSearchParams.copy(filters=filters)
      val query = getJsSearchQuery(params)
      val boolMust = readObjectArray(query, "query", "bool", "must")
      val queryString =
        boolMust.flatMap(obj => readObject(obj, "query_string")).head
      val traversed = readString(queryString, "query")
      assert(traversed == expected)
    }

    "specify field to search" in {
      val expected = Seq("genre")
      val filters = Seq(FieldFilter("sourceResource.subject.name", "london"))
      val params = minSearchParams.copy(filters=filters)
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
        val traversed = readString(queryTerm, "genre.not_analyzed")
        assert(traversed == expected)
      }

      "strip leading and trailing quotation marks from term" in {
        val expected = Some("Mystery fiction")
        val filters =
          Seq(FieldFilter("sourceResource.subject.name", "\"Mystery fiction\""))
        val params = minSearchParams.copy(filters=filters, exactFieldMatch=true)
        val query = getJsSearchQuery(params)
        val boolMust = readObjectArray(query, "query", "bool", "must")
        val queryTerm =
          boolMust.flatMap(obj => readObject(obj, "term")).head
        val traversed = readString(queryTerm, "genre.not_analyzed")
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
      val expected = Some("genre.not_analyzed")
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
        .flatMap(obj => readObject(obj, "title.not_analyzed")).head
      val traversed = readString(score, "order")
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
      val expected = "title"
      val traversed = readStringArray(detailQuery, "_source")
      traversed should contain only expected
    }
  }
}
