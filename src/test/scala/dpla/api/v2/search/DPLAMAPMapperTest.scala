package dpla.api.v2.search

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.api.helpers.FileReader
import dpla.api.v2.search.SearchProtocol.{DPLAMAPFetchResult, DPLAMAPMultiFetchResult, DPLAMAPSearchResult, FetchQueryResponse, IntermediateSearchResult, MultiFetchQueryResponse, SearchFailure, SearchQueryResponse, SearchResponse}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DPLAMAPMapperTest
  extends AnyWordSpec with Matchers with FileReader
  with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  val itemMapper: ActorRef[IntermediateSearchResult] =
    testKit.spawn(DPLAMAPMapper())

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
    readFile("/elasticSearchMinimalItemList.json")
  val esItem: String =
    readFile("/elasticSearchItem.json")

  "search response mapper" should {
    "return success for mappable response" in {
      itemMapper ! SearchQueryResponse(params, minEsEbookList, probe.ref)
      probe.expectMessageType[DPLAMAPSearchResult]
    }

    "return failure for unmappable response" in {
      val unmappable: String = ""
      itemMapper ! SearchQueryResponse(params, unmappable, probe.ref)
      probe.expectMessage(SearchFailure)
    }
  }

  "fetch response mapper" should {
    "return success for mappable response" in {
      itemMapper ! FetchQueryResponse(esItem, probe.ref)
      probe.expectMessageType[DPLAMAPFetchResult]
    }

    "fetch failure for unmappable response" in {
      val unmappable: String = ""
      itemMapper ! FetchQueryResponse(unmappable, probe.ref)
      probe.expectMessage(SearchFailure)
    }
  }

  "multi-fetch response mapper" should {
    "return success for mappable response" in {
      itemMapper ! MultiFetchQueryResponse(minEsEbookList, probe.ref)
      probe.expectMessageType[DPLAMAPMultiFetchResult]
    }

    "fetch failure for unmappable response" in {
      val unmappable: String = ""
      itemMapper ! FetchQueryResponse(unmappable, probe.ref)
      probe.expectMessage(SearchFailure)
    }
  }
}
