package dpla.api.v2.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.helpers.Utils.fakeApiKey
import dpla.api.v2.analytics.AnalyticsClient
import dpla.api.v2.analytics.AnalyticsClient.AnalyticsClientCommand
import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
import dpla.api.v2.authentication.{MockAuthenticator, MockPostgresClientSuccess}
import dpla.api.v2.registry.{ApiKeyRegistryCommand, MockApiKeyRegistry, MockEbookRegistry, MockItemRegistry, MockPssRegistry, SearchRegistryCommand}
import dpla.api.v2.search.{MockEbookSearch, MockItemSearch}
import dpla.api.v2.search.SearchProtocol.SearchCommand
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InvalidParamsTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit

  implicit def typedSystem: ActorSystem[Nothing] =
    testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val analyticsClient: ActorRef[AnalyticsClientCommand] =
    testKit.spawn(AnalyticsClient())
  val postgresClient = testKit.spawn(MockPostgresClientSuccess())

  val authenticator: ActorRef[AuthenticationCommand] =
    MockAuthenticator(testKit, Some(postgresClient))

  val ebookSearch: ActorRef[SearchCommand] =
    MockEbookSearch(testKit)

  val ebookRegistry: ActorRef[SearchRegistryCommand] =
    MockEbookRegistry(testKit, authenticator, analyticsClient, Some(ebookSearch))

  val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
    MockApiKeyRegistry(testKit, authenticator)

  val itemRegistry: ActorRef[SearchRegistryCommand] =
    MockItemRegistry(testKit, authenticator, analyticsClient)

  val pssRegistry: ActorRef[SearchRegistryCommand] =
    MockPssRegistry(testKit, authenticator, analyticsClient)

  lazy val routes: Route =
    new Routes(ebookRegistry, itemRegistry, pssRegistry, apiKeyRegistry)
      .applicationRoutes

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

  "/api_key/[email]" should {
    "return BadRequest if email is invalid" in {
      val request = Post("/v2/api_key/foo")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
        contentType should === (ContentTypes.`application/json`)
      }
    }
  }
}
