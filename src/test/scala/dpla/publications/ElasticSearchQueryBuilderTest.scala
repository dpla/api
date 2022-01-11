package dpla.publications

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.PrivateMethodTester
import spray.json._

class ElasticSearchQueryBuilderTest extends AnyWordSpec with Matchers with PrivateMethodTester with JsonFieldReader {

  val minSearchParams: SearchParams = SearchParams(
    facets = None,
    facetSize = 100,
    page = 3,
    pageSize = 20,
    q = None
  )
  val minQuery: JsObject = ElasticSearchQueryBuilder.composeQuery(minSearchParams).asJsObject

  val detailQueryParams: SearchParams = SearchParams(
    facets = Some(Seq("dataProvider", "sourceResource.publisher", "sourceResource.subject.name")),
    facetSize = 100,
    page = 3,
    pageSize = 20,
    q = Some("dogs")
  )
  val detailQuery: JsObject = ElasticSearchQueryBuilder.composeQuery(detailQueryParams).asJsObject

  val dplaToElasticSearch: PrivateMethod[String] = PrivateMethod[String](Symbol("dplaToElasticSearch"))

  "query builder" should {
    "specify from" in {
      val expected = 40
      val traversed = readInt(minQuery, "from").getOrElse("NOT FOUND")
      assert(traversed == expected)
    }

    "specify size" in {
      val expected = 20
      val traversed = readInt(minQuery, "size").getOrElse("NOT FOUND")
      assert(traversed == expected)
    }
  }

  "keyword query builder" should {
    "handle missing q" in {
      val expected = JsObject()
      val traversed = readObject(minQuery, "query", "match_all").get
      assert(traversed == expected)
    }

    "specify keyword" in {
      val expected = "dogs"
      val traversed = readString(detailQuery, "query", "query_string", "query")
        .getOrElse("NOT FOUND")
      assert(traversed == expected)
    }

    "specify fields to search" in {
      val traversed = readStringArray(detailQuery, "query", "query_string", "fields")
      assert(traversed.nonEmpty)
    }

    "specify wildcard analyzer" in {
      val expected = true
      val traversed = readBoolean(detailQuery, "query", "query_string", "analyze_wildcard").get
      assert(traversed == expected)
    }

    "specify default operator" in {
      val expected = "AND"
      val traversed = readString(detailQuery, "query", "query_string", "default_operator")
        .getOrElse("NOT FOUND")
      assert(traversed == expected)
    }

    "specify lenient" in {
      val expected = true
      val traversed = readBoolean(detailQuery, "query", "query_string", "lenient").get
      assert(traversed == expected)
    }
  }

  "agg query builder" should {
    "handle missing facets" in {
      val parent = readObject(minQuery, "aggs")
      val fieldNames = parent.get.fields.keys
      assert(fieldNames.isEmpty)
    }

    "include all facets" in {
      val expected = Seq("dataProvider", "sourceResource.publisher", "sourceResource.subject.name")
      val parent = readObject(detailQuery, "aggs")
      val fieldNames = parent.get.fields.keys
      fieldNames should contain allElementsOf expected
    }

    "specify facet field" in {
      val expected = "genre"
      val traversed = readString(detailQuery, "aggs", "sourceResource.subject.name", "terms", "field")
        .getOrElse("NOT FOUND")
      assert(traversed == expected)
    }

    "specify facet size" in {
      val expected = Some(100)
      val traversed = readInt(detailQuery, "aggs", "sourceResource.subject.name", "terms", "size")
      assert(traversed == expected)
    }
  }

  "field mapper" should {
    "map DPLA MAP fields to ElasticSearch fields" in {
      val dplaFields = Seq(
        "dataProvider",
        "isShownAt",
        "object",
        "sourceResource.creator",
        "sourceResource.date",
        "sourceResource.description",
        "sourceResource.format",
        "sourceResource.language.name",
        "sourceResource.publisher",
        "sourceResource.subject.name",
        "sourceResource.subtitle",
        "sourceResource.title"
      )
      val expected = Seq(
        "sourceUri",
        "itemUri",
        "payloadUri",
        "author",
        "publicationDate",
        "summary",
        "medium",
        "language",
        "publisher",
        "genre",
        "subtitle",
        "title"
      )
      val mapped = dplaFields.map(field => ElasticSearchQueryBuilder invokePrivate dplaToElasticSearch(field))
      mapped should contain allElementsOf expected
    }
  }
}