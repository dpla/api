package dpla.api.v2.search

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.api.helpers.FileReader
import dpla.api.v2.search.SearchProtocol.{EbookFetchResult, EbookMultiFetchResult, EbookSearchResult, FetchQueryResponse, IntermediateSearchResult, MultiFetchQueryResponse, SearchFailure, SearchQueryResponse, SearchResponse}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EbookMapperTest extends AnyWordSpec with Matchers with FileReader
  with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  val ebookMapper: ActorRef[IntermediateSearchResult] =
    testKit.spawn(EbookMapper())

  val probe: TestProbe[SearchResponse] =
    testKit.createTestProbe[SearchResponse]

  val params: SearchParams = SearchParams(
    exactFieldMatch = false,
    facets = None,
    facetSize = 10,
    fields = None,
    filters = Seq(),
    op = "",
    page = 1,
    pageSize = 10,
    q = None,
    sortBy = None,
    sortOrder = ""
  )

  val minEsEbookList: String =
    readFile("/elasticSearchMinimalEbookList.json")
  val minEsEbook: String =
    readFile("/elasticSearchMinimalEbook.json")

  "search response mapper" should {
    "return success for mappable response" in {
      ebookMapper ! SearchQueryResponse(params, minEsEbookList, probe.ref)
      probe.expectMessageType[EbookSearchResult]
    }

    "return failure for unmappable response" in {
      val unmappable: String = ""
      ebookMapper ! SearchQueryResponse(params, unmappable, probe.ref)
      probe.expectMessage(SearchFailure)
    }
  }

  "fetch response mapper" should {
    "return success for mappable response" in {
      ebookMapper ! FetchQueryResponse(minEsEbook, probe.ref)
      probe.expectMessageType[EbookFetchResult]
    }

    "fetch failure for unmappable response" in {
      val unmappable: String = ""
      ebookMapper ! FetchQueryResponse(unmappable, probe.ref)
      probe.expectMessage(SearchFailure)
    }
  }

  "multi-fetch response mapper" should {
    "return success for mappable response" in {
      ebookMapper ! MultiFetchQueryResponse(minEsEbookList, probe.ref)
      probe.expectMessageType[EbookMultiFetchResult]
    }

    "fetch failure for unmappable response" in {
      val unmappable: String = ""
      ebookMapper ! FetchQueryResponse(unmappable, probe.ref)
      probe.expectMessage(SearchFailure)
    }
  }
}
