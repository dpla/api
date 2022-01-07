package dpla.publications

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.PrivateMethodTester
import spray.json._

class ElasticSearchClientTest extends AnyWordSpec with Matchers with PrivateMethodTester with JsonFieldReader {

  val client = new ElasticSearchClient("http://localhost:9200/eleanor")

  "query composer" should {
    val composeQuery = PrivateMethod[JsValue](Symbol("composeQuery"))
    val minSearchParams: SearchParams = SearchParams(page = 3, pageSize = 20)

    "specify from" in {
      val expected = 40
      val params = minSearchParams
      val query: JsObject = client.invokePrivate(composeQuery(params)).asJsObject
      val traversed = readInt(query, "from").getOrElse("NOT FOUND")
      assert(traversed == expected)
    }

    "specify size" in {
      val expected = 20
      val params = minSearchParams
      val query: JsObject = client.invokePrivate(composeQuery(params)).asJsObject
      val traversed = readInt(query, "size").getOrElse("NOT FOUND")
      assert(traversed == expected)
    }
  }

}
