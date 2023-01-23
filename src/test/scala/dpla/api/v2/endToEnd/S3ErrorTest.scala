package dpla.api.v2.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, StatusCodes}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.helpers.ActorHelper
import dpla.api.helpers.Utils.fakeApiKey
import dpla.api.v2.registry._
import dpla.api.v2.search.SearchProtocol.SearchCommand
import dpla.api.v2.search.MockEbookSearch
import dpla.api.v2.smr.MockSmrRequestHandler
import dpla.api.v2.smr.SmrProtocol.SmrCommand
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._

class S3ErrorTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest with ActorHelper with DefaultJsonProtocol {

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

      val service = "tiktok"
      val post = "123"
      val user = "abc"

      val postData = JsObject(
        "service" -> service.toJson,
        "post" -> post.toJson,
        "user" -> user.toJson
      ).toString

      val request = HttpRequest(
        POST,
        uri = "/v2/smr",
        entity = HttpEntity(ContentTypes.`application/json`, postData),
        headers = List(RawHeader("Authorization", fakeApiKey))
      )

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.InternalServerError
      }
    }
  }
}
