package dpla.api.v2.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.helpers.ActorHelper
import dpla.api.helpers.Utils.fakeApiKey
import dpla.api.v2.registry.{MockEbookRegistry, MockSmrRegistry, SearchRegistryCommand, SmrRegistryCommand}
import dpla.api.v2.search.MockEbookSearch
import dpla.api.v2.search.SearchProtocol.SearchCommand
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

  val ebookSearch: ActorRef[SearchCommand] =
    MockEbookSearch(testKit)

  val ebookRegistry: ActorRef[SearchRegistryCommand] =
    MockEbookRegistry(testKit, authenticator, ebookAnalyticsClient, Some(ebookSearch))

  val smrRequestHandler: ActorRef[SmrCommand] =
    MockSmrRequestHandler(testKit, Some(s3ClientSuccess))

  val smrRegistryS3Success: ActorRef[SmrRegistryCommand] =
    MockSmrRegistry(testKit, authenticator, Some(smrRequestHandler))

  lazy val routes: Route =
    new Routes(ebookRegistry, itemRegistry, pssRegistry, apiKeyRegistry,
      smrRegistryS3Success).applicationRoutes

  "/v2/ebooks route" should {
    "return BadRequest if params are invalid" in {
      val request = Get(s"/v2/ebooks?page=foo&api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
        contentType should === (ContentTypes.`application/json`)
      }
    }

    "ignore empty params" in {
      val request = Get(s"/v2/ebooks?page=&api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status should not be StatusCodes.BadRequest
        contentType should === (ContentTypes.`application/json`)
      }
    }
  }

  "/v2/ebooks[id] route" should {
    "return BadRequest if id is invalid" in {
      val request = Get(s"/v2/ebooks/<foo>?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
        contentType should === (ContentTypes.`application/json`)
      }
    }

    "return BadRequest if params are invalid" in {
      val request =
        Get(s"/v2/ebooks/R0VfVX4BfY91SSpFGqxt?foo=bar&api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
        contentType should === (ContentTypes.`application/json`)
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
}
