package dpla.ebookapi.v1.ebooks

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import dpla.ebookapi.helpers.FileReader
import dpla.ebookapi.v1.ebooks.EbookMapper.{MapFetchResponse, MapSearchResponse}
import org.scalatest.BeforeAndAfterAll

class EbookMapperTest extends AnyWordSpec with Matchers with FileReader with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()
  val ebookMapper: ActorRef[EbookMapper.MapperCommand] = testKit.spawn(EbookMapper())
  val probe: TestProbe[EbookMapperResponse] = testKit.createTestProbe[EbookMapperResponse]()

  val page = 1
  val pageSize = 10
  val minEsEbookList: String = readFile("/elasticSearchMinimalEbookList.json")
  val minEsEbook: String = readFile("/elasticSearchMinimalEbook.json")

  "search response mapper" should {
    "return success for mappable response" in {
      ebookMapper ! MapSearchResponse(minEsEbookList, page, pageSize, probe.ref)
      probe.expectMessageType[MappedEbookList]
    }

    "return failure for unmappable response" in {
      val unmappable: String = ""
      ebookMapper ! MapSearchResponse(unmappable, page, pageSize, probe.ref)
      probe.expectMessage(MapFailure)
    }
  }

  "fetch response mapper" should {
    "return success for mappable response" in {
      ebookMapper ! MapFetchResponse(minEsEbook, probe.ref)
      probe.expectMessageType[MappedSingleEbook]
    }

    "fetch failure for unmappable response" in {
      val unmappable: String = ""
      ebookMapper ! MapFetchResponse(unmappable, probe.ref)
      probe.expectMessage(MapFailure)
    }
  }
}
