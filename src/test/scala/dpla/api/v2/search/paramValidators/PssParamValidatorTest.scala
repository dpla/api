package dpla.api.v2.search.paramValidators

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.api.v2.search.SearchProtocol._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec


class PssParamValidatorTest extends AnyWordSpec with Matchers
  with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  val replyProbe: TestProbe[SearchResponse] =
    testKit.createTestProbe[SearchResponse]

  val interProbe: TestProbe[IntermediateSearchResult] =
    testKit.createTestProbe[IntermediateSearchResult]

  val pssParamValidator: ActorRef[IntermediateSearchResult] =
    testKit.spawn(PssParamValidator(interProbe.ref))

  "page size validator" should {
    "set default page size to 200" in {
      val expected = 200
      pssParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.pageSize shouldEqual expected
    }
  }

  "fields validator" should {
    "set default search fields" in {
      pssParamValidator ! RawSearchParams(Map(), replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.fields should not be empty
    }
  }

  "id validator" should {
    "accept a valid string" in {
      val expected = "immigration-through-angel-island"
      val params = Map("id" -> "immigration-through-angel-island")
      pssParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val id = msg.params.fieldQueries.find(_.fieldName == "id")
        .map(_.value)
      id should contain(expected)
    }

    "reject an invalid string" in {
      val params = Map("id" -> ".")
      pssParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }

  "hasPart.id validator" should {
    "accept an integer" in {
      val expected = "7"
      val params = Map("hasPart.id" -> "7")
      pssParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      val id = msg.params.fieldQueries.find(_.fieldName == "hasPart.id")
        .map(_.value)
      id should contain(expected)
    }

    "reject a non-integer" in {
      val params = Map("hasPart.id" -> "foo")
      pssParamValidator ! RawSearchParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSearchParams]
    }
  }
}
