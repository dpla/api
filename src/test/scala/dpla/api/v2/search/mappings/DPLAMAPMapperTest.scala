package dpla.api.v2.search.mappings

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.api.helpers.FileReader
import dpla.api.v2.search.SearchProtocol._
import dpla.api.v2.search.paramValidators.{RandomParams, SearchParams}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DPLAMAPMapperTest
  extends AnyWordSpec with Matchers with FileReader with JsonFieldReader
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
    fieldQueries = Seq(),
    filter = None,
    op = "",
    page = 1,
    pageSize = 10,
    q = None,
    sortBy = None,
    sortByPin = None,
    sortOrder = ""
  )

  val minEsEbookList: String =
    readFile("/elasticSearchMinimalItemList.json")
  val esItem: String =
    readFile("/elasticSearchItem.json")
  val itemList: String =
    readFile("/elasticSearchItemList.json")

  "search response mapper" should {
    "return success for mappable response" in {
      itemMapper ! SearchQueryResponse(params, minEsEbookList, probe.ref)
      probe.expectMessageType[MappedSearchResult]
    }

    "return failure for unmappable response" in {
      val unmappable: String = ""
      itemMapper ! SearchQueryResponse(params, unmappable, probe.ref)
      probe.expectMessage(SearchFailure)
    }

    "un-nest field paths with literals" in {
      val expected = Some("California Digital Library")
      val fieldsParams = params.copy(fields = Some(Seq("provider.name")))
      itemMapper ! SearchQueryResponse(fieldsParams, itemList, probe.ref)
      val msg = probe.expectMessageType[MappedSearchResult]
      val firstEntry = msg.mappedDocList.asInstanceOf[DPLADocList].docs.head
      val traversed = readString(firstEntry.asJsObject, "provider.name")
      assert(traversed == expected)
    }

    "un-nest field paths with internal arrays" in {
      val expected = Seq(
        "Children",
        "Dwarf hamsters",
        "Saint Vincent Center (Los Angeles)"
      )
      val fieldsParams = params.copy(fields = Some(Seq("sourceResource.subject.name")))
      itemMapper ! SearchQueryResponse(fieldsParams, itemList, probe.ref)
      val msg = probe.expectMessageType[MappedSearchResult]
      val firstEntry = msg.mappedDocList.asInstanceOf[DPLADocList].docs.head
      val traversed = readStringArray(firstEntry.asJsObject, "sourceResource.subject.name")
      traversed should contain allElementsOf expected
    }

    "un-nest field paths with objects" in {
      val expected = Some("circa 1996")
      val fieldsParams = params.copy(fields = Some(Seq("sourceResource.temporal")))
      itemMapper ! SearchQueryResponse(fieldsParams, itemList, probe.ref)
      val msg = probe.expectMessageType[MappedSearchResult]
      val firstEntry = msg.mappedDocList.asInstanceOf[DPLADocList].docs.head
      val firstTemporal =
        readObject(firstEntry.asJsObject, "sourceResource.temporal").get
      val traversed = readString(firstTemporal, "displayDate")
      assert(traversed == expected)
    }

    "collapse arrays with a single element" in {
      val expected = Some("Children play with hamsters, Saint Vincent Center, Los Angeles, 1996")
      val fieldsParams = params.copy(fields = Some(Seq("sourceResource.title")))
      itemMapper ! SearchQueryResponse(fieldsParams, itemList, probe.ref)
      val msg = probe.expectMessageType[MappedSearchResult]
      val firstEntry = msg.mappedDocList.asInstanceOf[DPLADocList].docs.head
      val traversed = readString(firstEntry.asJsObject, "sourceResource.title")
      assert(traversed == expected)
    }
  }

  "fetch response mapper" should {
    "return success for mappable response" in {
      itemMapper ! FetchQueryResponse(None, esItem, probe.ref)
      probe.expectMessageType[MappedFetchResult]
    }

    "return failure for unmappable response" in {
      val unmappable: String = ""
      itemMapper ! FetchQueryResponse(None, unmappable, probe.ref)
      probe.expectMessage(SearchFailure)
    }
  }

  "multi-fetch response mapper" should {
    "return success for mappable response" in {
      itemMapper ! MultiFetchQueryResponse(minEsEbookList, probe.ref)
      probe.expectMessageType[MappedMultiFetchResult]
    }

    "return failure for unmappable response" in {
      val unmappable: String = ""
      itemMapper ! MultiFetchQueryResponse(unmappable, probe.ref)
      probe.expectMessage(SearchFailure)
    }
  }

  "random response mapper" should {
    "return success for mappable response" in {
      itemMapper ! RandomQueryResponse(RandomParams(), itemList, probe.ref)
      probe.expectMessageType[MappedRandomResult]
    }

    "return failure for unmappable response" in {
      val unmappable: String = ""
      itemMapper ! RandomQueryResponse(RandomParams(), unmappable, probe.ref)
      probe.expectMessage(SearchFailure)
    }
  }
}
