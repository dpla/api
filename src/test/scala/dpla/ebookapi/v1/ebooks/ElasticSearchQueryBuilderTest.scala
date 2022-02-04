package dpla.ebookapi.v1.ebooks

import org.scalatest.PrivateMethodTester
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._

class ElasticSearchQueryBuilderTest extends AnyWordSpec with Matchers with PrivateMethodTester with JsonFieldReader {

  class ElasticSearchQueryBuilderTester extends ElasticSearchQueryBuilder
  val tester = new ElasticSearchQueryBuilderTester

  val minSearchParams: SearchParams = SearchParams(
    exactFieldMatch = false,
    facets = None,
    facetSize = 100,
    filters = Seq[FieldFilter](),
    page = 3,
    pageSize = 20,
    q = None
  )
  val minQuery: JsObject = tester.composeQuery(minSearchParams).asJsObject

  val detailQueryParams: SearchParams = SearchParams(
    exactFieldMatch = false,
    facets = Some(Seq("provider.@id", "sourceResource.publisher", "sourceResource.subject.name")),
    facetSize = 100,
    filters = Seq(FieldFilter("sourceResource.subject.name", "adventure")),
    page = 3,
    pageSize = 20,
    q = Some("dogs")
  )
  val detailQuery: JsObject = tester.composeQuery(detailQueryParams).asJsObject

  val elasticSearchField: PrivateMethod[String] =
    PrivateMethod[String](Symbol("elasticSearchField"))

  val exactMatchElasticSearchField: PrivateMethod[String] =
    PrivateMethod[String](Symbol("exactMatchElasticSearchField"))

  "query builder" should {
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
      val queryString = boolMust.flatMap(obj => readObject(obj, "query_string")).head
      val traversed = readString(queryString, "query")
      assert(traversed == expected)
    }

    "specify fields to search" in {
      val boolMust = readObjectArray(detailQuery, "query", "bool", "must")
      val queryString = boolMust.flatMap(obj => readObject(obj, "query_string")).head
      val traversed = readStringArray(queryString, "fields")
      assert(traversed.nonEmpty)
    }

    "specify wildcard analyzer" in {
      val expected = Some(true)
      val boolMust = readObjectArray(detailQuery, "query", "bool", "must")
      val queryString = boolMust.flatMap(obj => readObject(obj, "query_string")).head
      val traversed = readBoolean(queryString, "analyze_wildcard")
      assert(traversed == expected)
    }

    "specify default operator" in {
      val expected = Some("AND")
      val boolMust = readObjectArray(detailQuery, "query", "bool", "must")
      val queryString = boolMust.flatMap(obj => readObject(obj, "query_string")).head
      val traversed = readString(queryString, "default_operator")
      assert(traversed == expected)
    }

    "specify lenient" in {
      val expected = Some(true)
      val boolMust = readObjectArray(detailQuery, "query", "bool", "must")
      val queryString = boolMust.flatMap(obj => readObject(obj, "query_string")).head
      val traversed = readBoolean(queryString, "lenient")
      assert(traversed == expected)
    }
  }

  "field filter query builder" should {
    "handle no field search with q" in {
      val params = minSearchParams.copy(q=Some("dogs"))
      val query = tester.composeQuery(params).asJsObject
      val boolMust = readObjectArray(query, "query", "bool", "must")
      val queryString = boolMust.flatMap(obj => readObject(obj, "query_string"))
      assert(queryString.size == 1)
    }

    "handle field search with no q" in {
      val filters = Seq(FieldFilter("sourceResource.subject.name", "london"))
      val params = minSearchParams.copy(filters=filters)
      val query = tester.composeQuery(params).asJsObject
      val boolMust = readObjectArray(query, "query", "bool", "must")
      val queryString = boolMust.flatMap(obj => readObject(obj, "query_string"))
      assert(queryString.size == 1)
    }

    "handle multiple field searches" in {
      val filters = Seq(
        FieldFilter("sourceResource.subject.name", "london"),
        FieldFilter("provider.@id", "http://standardebooks.org")
      )
      val params = minSearchParams.copy(filters=filters)
      val query = tester.composeQuery(params).asJsObject
      val boolMust = readObjectArray(query, "query", "bool", "must")
      val queryMatch = boolMust.flatMap(obj => readObject(obj, "query_string"))
      assert(queryMatch.size == 2)
    }

    "specify filter term" in {
      val expected = Some("london")
      val filters = Seq(FieldFilter("sourceResource.subject.name", "london"))
      val params = minSearchParams.copy(filters=filters)
      val query = tester.composeQuery(params).asJsObject
      val boolMust = readObjectArray(query, "query", "bool", "must")
      val queryString = boolMust.flatMap(obj => readObject(obj, "query_string")).head
      val traversed = readString(queryString, "query")
      assert(traversed == expected)
    }

    "specify field to search" in {
      val expected = Seq("genre")
      val filters = Seq(FieldFilter("sourceResource.subject.name", "london"))
      val params = minSearchParams.copy(filters=filters)
      val query = tester.composeQuery(params).asJsObject
      val boolMust = readObjectArray(query, "query", "bool", "must")
      val queryString = boolMust.flatMap(obj => readObject(obj, "query_string")).head
      val traversed = readStringArray(queryString, "fields")
      assert(traversed == expected)
    }

    "exact term match" should {

      "use 'term' query" in {
        val params = detailQueryParams.copy(exactFieldMatch = true)
        val query = tester.composeQuery(params).asJsObject
        val boolMust = readObjectArray(query, "query", "bool", "must")
        val queryTerm = boolMust.flatMap(obj => readObject(obj, "term"))
        assert(queryTerm.size == 1)
      }

      "specify exact field match field and term" in {
        val expected = Some("adventure")
        val params = detailQueryParams.copy(exactFieldMatch = true)
        val query = tester.composeQuery(params).asJsObject
        val boolMust = readObjectArray(query, "query", "bool", "must")
        val queryTerm = boolMust.flatMap(obj => readObject(obj, "term")).head
        val traversed = readString(queryTerm, "genre.not_analyzed")
        assert(traversed == expected)
      }

      "strip leading and trailing quotation marks from term" in {
        val expected = Some("Mystery fiction")
        val filters = Seq(FieldFilter("sourceResource.subject.name", "\"Mystery fiction\""))
        val params = minSearchParams.copy(filters=filters, exactFieldMatch=true)
        val query = tester.composeQuery(params).asJsObject
        val boolMust = readObjectArray(query, "query", "bool", "must")
        val queryTerm = boolMust.flatMap(obj => readObject(obj, "term")).head
        val traversed = readString(queryTerm, "genre.not_analyzed")
        assert(traversed == expected)
      }
    }
  }

  "agg query builder" should {
    "handle missing facets" in {
      val parent = readObject(minQuery, "aggs")
      val fieldNames = parent.get.fields.keys
      assert(fieldNames.isEmpty)
    }

    "include all facets" in {
      val expected = Seq("provider.@id", "sourceResource.publisher", "sourceResource.subject.name")
      val parent = readObject(detailQuery, "aggs")
      val fieldNames = parent.get.fields.keys
      fieldNames should contain allElementsOf expected
    }

    "specify facet field" in {
      val expected = Some("genre.not_analyzed")
      val traversed = readString(detailQuery, "aggs", "sourceResource.subject.name", "terms", "field")
      assert(traversed == expected)
    }

    "specify facet size" in {
      val expected = Some(100)
      val traversed = readInt(detailQuery, "aggs", "sourceResource.subject.name", "terms", "size")
      assert(traversed == expected)
    }
  }
}
