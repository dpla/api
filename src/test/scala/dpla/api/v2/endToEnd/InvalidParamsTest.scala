package dpla.api.v2.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.helpers.ActorHelper
import dpla.api.helpers.Utils.fakeApiKey
import dpla.api.v2.registry.{MockItemRegistry, MockSmrRegistry, SmrRegistryCommand}
import dpla.api.v2.search.{MockEsClientQueryParseError, MockItemSearch}
import dpla.api.v2.smr.MockSmrRequestHandler
import dpla.api.v2.smr.SmrProtocol.SmrCommand
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InvalidParamsTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest with ActorHelper {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit

  implicit def typedSystem: ActorSystem[Nothing] =
    testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val smrRequestHandler: ActorRef[SmrCommand] =
    MockSmrRequestHandler(testKit, Some(s3ClientSuccess))

  val smrRegistryS3Success: ActorRef[SmrRegistryCommand] =
    MockSmrRegistry(testKit, authenticator, Some(smrRequestHandler))

  lazy val routes: Route =
    new Routes(itemRegistry, pssRegistry, apiKeyRegistry,
      smrRegistryS3Success).applicationRoutes

  val esQueryParseErrorClient =
    testKit.spawn(MockEsClientQueryParseError())
  val itemSearchWithParseError =
    MockItemSearch(testKit, Some(esQueryParseErrorClient))
  val itemRegistryWithParseError =
    MockItemRegistry(testKit, authenticator, itemAnalyticsClient,
      Some(itemSearchWithParseError))
  val routesWithParseError: Route =
    new Routes(itemRegistryWithParseError, pssRegistry, apiKeyRegistry,
      smrRegistryS3Success).applicationRoutes

  "malformed api_key" should {
    "return Forbidden for a key that is too short" in {
      val request = Get("/v2/items?api_key=tooshort")
      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
        contentType should === (ContentTypes.`application/json`)
      }
    }

    "return Forbidden for a key with invalid characters" in {
      val request = Get("/v2/items?api_key=your_dpla_api_key_here__________")
      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
        contentType should === (ContentTypes.`application/json`)
      }
    }

    "return Forbidden for a key that is too long" in {
      val request = Get(s"/v2/items?api_key=${fakeApiKey}extra")
      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
        contentType should === (ContentTypes.`application/json`)
      }
    }

    "still pass a well-formed key through to the registry" in {
      val request = Get(s"/v2/items?api_key=$fakeApiKey")
      request ~> Route.seal(routes) ~> check {
        status should not be StatusCodes.Forbidden
        status should not be StatusCodes.NotFound
      }
    }
  }

  "/v2/smr" should {
    "return BadRequest if params are invalid" in {
      val validService = "invalid"
      val validPost = "123"
      val validUser = "abc"

      val request = Post(s"/v2/smr?api_key=$fakeApiKey&service=$validService&post=$validPost&user=$validUser")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "/v2/api_key/[email]" should {
    "return BadRequest if email is invalid" in {
      val request = Post("/v2/api_key/foo")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
        contentType should === (ContentTypes.`application/json`)
      }
    }
  }

  "Elasticsearch query parse error" should {
    "return BadRequest for /v2/items" in {
      val request = Get(s"/v2/items?api_key=$fakeApiKey&q=invalid%29-")
      request ~> Route.seal(routesWithParseError) ~> check {
        status shouldEqual StatusCodes.BadRequest
        contentType should === (ContentTypes.`application/json`)
      }
    }
  }
}
