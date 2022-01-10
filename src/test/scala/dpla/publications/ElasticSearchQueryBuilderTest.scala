package dpla.publications

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.PrivateMethodTester
import spray.json._

class ElasticSearchQueryBuilderTest extends AnyWordSpec with Matchers with PrivateMethodTester with JsonFieldReader {

  val minSearchParams: SearchParams = SearchParams(
    facets = None,
    page = 3,
    pageSize = 20,
    q = None
  )
  val minQuery: JsObject = ElasticSearchQueryBuilder.composeQuery(minSearchParams).asJsObject

  val detailQueryParams: SearchParams = SearchParams(
    facets = Some(Seq("dataProvider", "sourceResource.subject.name")),
    page = 3,
    pageSize = 20,
    q = Some("dogs")
  )
  val detailQuery: JsObject = ElasticSearchQueryBuilder.composeQuery(detailQueryParams).asJsObject

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
}
