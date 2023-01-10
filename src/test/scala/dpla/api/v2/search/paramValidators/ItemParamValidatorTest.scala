package dpla.api.v2.search.paramValidators

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.api.v2.search.SearchProtocol.{IntermediateSearchResult, RawSearchParams, SearchResponse, ValidSearchParams}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ItemParamValidatorTest extends AnyWordSpec with Matchers
  with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  val replyProbe: TestProbe[SearchResponse] =
    testKit.createTestProbe[SearchResponse]

  val interProbe: TestProbe[IntermediateSearchResult] =
    testKit.createTestProbe[IntermediateSearchResult]

  val itemParamValidator: ActorRef[IntermediateSearchResult] =
    testKit.spawn(ItemParamValidator(interProbe.ref))

  "facet validator" should {
    "ignore valid DPLA Map fields not applicable to items" in {
      val given = "sourceResource.subtitle"
      val expected = Some(Seq())
      val params = Map("facets" -> given)
      itemParamValidator ! RawSearchParams(params, replyProbe.ref)
      val msg = interProbe.expectMessageType[ValidSearchParams]
      msg.params.facets shouldEqual expected
    }
  }
}
