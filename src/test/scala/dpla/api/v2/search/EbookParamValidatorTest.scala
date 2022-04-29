package dpla.api.v2.search

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.api.v2.search.SearchProtocol.{IntermediateSearchResult, InvalidSearchParams, RawSearchParams, SearchResponse, ValidSearchParams}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Random


class EbookParamValidatorTest extends AnyWordSpec with Matchers
  with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  val replyProbe: TestProbe[SearchResponse] =
    testKit.createTestProbe[SearchResponse]

  val interProbe: TestProbe[IntermediateSearchResult] =
    testKit.createTestProbe[IntermediateSearchResult]

  val ebookParamValidator: ActorRef[IntermediateSearchResult] =
    testKit.spawn(EbookParamValidator(interProbe.ref))

  "facet validator" should {
    "ignore valid DPLA Map fields not applicable to ebooks" in {
      val given = "rightsCategory"
      val expected = Some(Seq())
      val params = Map("facets" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.facets shouldEqual expected
    }
  }

  "subtitle validator" should {
    "handle empty param" in {
      val expected = None
      ebookParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue =msg.params.filters
        .find(_.fieldName == "sourceResource.subtitle")
      fieldValue shouldEqual expected
    }

    "accept valid param" in {
      val given = "A play in three acts"
      val expected = Some("A play in three acts")
      val params = Map("sourceResource.subtitle" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val fieldValue = msg.params.filters
        .find(_.fieldName == "sourceResource.subtitle").map(_.value)
      fieldValue shouldEqual expected
    }

    "reject too-short param" in {
      val given = "d"
      val params = Map("sourceResource.subtitle" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }

    "reject too-long param" in {
      val given: String = Random.alphanumeric.take(201).mkString
      val params = Map("sourceResource.subtitle" -> given)
      ebookParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }
}
