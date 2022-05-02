package dpla.api.v2.httpHeadersAndMethods

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.helpers.Utils.fakeApiKey
import dpla.api.v2.analytics.AnalyticsClient
import dpla.api.v2.analytics.AnalyticsClient.AnalyticsClientCommand
import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
import dpla.api.v2.authentication.{MockAuthenticator, MockPostgresClientSuccess}
import dpla.api.v2.registry.{ApiKeyRegistryCommand, MockApiKeyRegistry, MockEbookRegistry, MockItemRegistry, SearchRegistryCommand}
import dpla.api.v2.search.SearchProtocol.SearchCommand
import dpla.api.v2.search.{DPLAMAPMapper, MockEbookSearch, MockEsClientSuccess}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HeaderAuthorizationTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val analyticsClient: ActorRef[AnalyticsClientCommand] =
    testKit.spawn(AnalyticsClient())
  val postgresClient = testKit.spawn(MockPostgresClientSuccess())
  val mapper = testKit.spawn(DPLAMAPMapper())
  val elasticSearchClient = testKit.spawn(MockEsClientSuccess(mapper))

  val ebookSearch: ActorRef[SearchCommand] =
    MockEbookSearch(testKit, Some(elasticSearchClient), Some(mapper))

  val authenticator: ActorRef[AuthenticationCommand] =
    MockAuthenticator(testKit, Some(postgresClient))

  val ebookRegistry: ActorRef[SearchRegistryCommand] =
    MockEbookRegistry(testKit, authenticator, analyticsClient, Some(ebookSearch))

  val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
    MockApiKeyRegistry(testKit, authenticator)

  val itemRegistry: ActorRef[SearchRegistryCommand] =
    MockItemRegistry(testKit, authenticator, analyticsClient)

  lazy val routes: Route =
    new Routes(ebookRegistry, itemRegistry, apiKeyRegistry).applicationRoutes

  "/v2/ebooks route" should {
    "accept API key in HTTP header" in {
      val request = Get(s"/v2/ebooks")
        .withHeaders(RawHeader("Authorization", fakeApiKey))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "privilege API key in HTTP header over that in query" in {
      val request = Get(s"/v2/ebooks?api_key=foo")
        .withHeaders(RawHeader("Authorization", fakeApiKey))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "fail if API key is neither in HTTP header nor query" in {
      val request = Get(s"/v2/ebooks")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }
  }

  "/v2/ebooks[id] route" should {
    "accept API key in HTTP header" in {
      val request = Get(s"/v2/ebooks/R0VfVX4BfY91SSpFGqxt")
        .withHeaders(RawHeader("Authorization", fakeApiKey))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "privilege API key in HTTP header over that in query" in {
      val request = Get(s"/v2/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=foo")
        .withHeaders(RawHeader("Authorization", fakeApiKey))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "fail if API key is neither in HTTP header nor query" in {
      val request = Get(s"/v2/ebooks/R0VfVX4BfY91SSpFGqxt")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }
  }
}
