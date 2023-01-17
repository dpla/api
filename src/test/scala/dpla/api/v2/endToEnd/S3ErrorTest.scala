package dpla.api.v2.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.helpers.ActorHelper
import dpla.api.helpers.Utils.fakeApiKey
import dpla.api.v2.registry.{MockEbookRegistry, MockSmrRegistry, SearchRegistryCommand, SmrRegistryCommand}
import dpla.api.v2.search.SearchProtocol.SearchCommand
import dpla.api.v2.search.MockEbookSearch
import dpla.api.v2.smr.MockSmrRequestHandler
import dpla.api.v2.smr.SmrProtocol.SmrCommand
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class S3ErrorTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest with ActorHelper {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val ebookSearch: ActorRef[SearchCommand] =
    MockEbookSearch(testKit, Some(elasticSearchClientFailure))

  val ebookRegistry: ActorRef[SearchRegistryCommand] =
    MockEbookRegistry(testKit, authenticator, ebookAnalyticsClient, Some(ebookSearch))

  val smrRequestHandler: ActorRef[SmrCommand] =
    MockSmrRequestHandler(testKit, Some(s3ClientFailure))

  val smrRegistryS3Failure: ActorRef[SmrRegistryCommand] =
    MockSmrRegistry(testKit, authenticator, Some(smrRequestHandler))

  lazy val routes: Route =
    new Routes(ebookRegistry, itemRegistry, pssRegistry, apiKeyRegistry,
      smrRegistryS3Failure).applicationRoutes

  "/v2/smr route" should {
    "return InternalServerError if S3 upload fails" in {
      val validService = "tiktok"
      val validPost = "123"
      val validUser = "abc"

      val request = Post(s"/v2/smr?api_key=$fakeApiKey&service=$validService&post=$validPost&user=$validUser")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.InternalServerError
      }
    }
  }
}
