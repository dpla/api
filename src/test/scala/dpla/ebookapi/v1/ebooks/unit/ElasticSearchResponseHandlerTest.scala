package dpla.ebookapi.v1.ebooks.unit

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import dpla.ebookapi.v1.ebooks.ElasticSearchResponseHandler.{ElasticSearchResponseHandlerCommand, ProcessElasticSearchResponse}
import dpla.ebookapi.v1.ebooks._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{AsyncTestSuite, BeforeAndAfterAll}

import scala.concurrent.Future

class ElasticSearchResponseHandlerTest extends AnyWordSpec with Matchers with BeforeAndAfterAll with AsyncTestSuite {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  val responseHandler: ActorRef[ElasticSearchResponseHandlerCommand] =
    testKit.spawn(ElasticSearchResponseHandler())
  val probe: TestProbe[ElasticSearchResponse] = testKit.createTestProbe[ElasticSearchResponse]()

  "response processor" should {
    "handle successful response" in {
      val entity = "{\"foo\":\"bar\"}"
      val httpResponse = Future(HttpResponse(OK, entity=entity))
      responseHandler ! ProcessElasticSearchResponse(httpResponse, probe.ref)
      val response: ElasticSearchSuccess = probe.expectMessageType[ElasticSearchSuccess]
      assert(response.body == entity)
    }

    "handle http error" in {
      val httpResponse = Future(HttpResponse(NotFound))
      responseHandler ! ProcessElasticSearchResponse(httpResponse, probe.ref)
      val response: ElasticSearchHttpFailure = probe.expectMessageType[ElasticSearchHttpFailure]
      assert(response.statusCode.intValue == 404)
    }

    "handle unreachable endpoint" in {
      val httpResponse = Future.failed(new Exception)
      responseHandler ! ProcessElasticSearchResponse(httpResponse, probe.ref)
      probe.expectMessage(ElasticSearchUnreachable)
    }
  }
}
