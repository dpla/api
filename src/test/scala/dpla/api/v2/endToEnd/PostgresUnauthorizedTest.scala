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
import dpla.api.v2.authentication.{MockAuthenticator, MockPostgresClientDisabled, MockPostgresClientKeyNotFound}
import dpla.api.v2.registry.{ApiKeyRegistryCommand, SearchRegistryCommand, MockApiKeyRegistry, MockEbookRegistry}
import dpla.api.v2.search.SearchProtocol.SearchCommand
import dpla.api.v2.search.{DPLAMAPMapper, MockEbookSearch, MockEsClientSuccess}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PostgresUnauthorizedTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val analyticsClient: ActorRef[AnalyticsClientCommand] =
    testKit.spawn(AnalyticsClient())
  val mapper = testKit.spawn(DPLAMAPMapper())
  val elasticSearchClient = testKit.spawn(MockEsClientSuccess(mapper))

  val ebookSearch: ActorRef[SearchCommand] =
    MockEbookSearch(testKit, Some(elasticSearchClient), Some(mapper))

  "/v2/ebooks route" should {
    "return Forbidden if API key not found" in {
      val postgresClient = testKit.spawn(MockPostgresClientKeyNotFound())

      val authenticator: ActorRef[AuthenticationCommand] =
        MockAuthenticator(testKit, Some(postgresClient))

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, analyticsClient, Some(ebookSearch))

      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        MockApiKeyRegistry(testKit, authenticator)

      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v2/ebooks?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "return Forbidden if API key disabled" in {
      val postgresClient = testKit.spawn(MockPostgresClientDisabled())

      val authenticator: ActorRef[AuthenticationCommand] =
        MockAuthenticator(testKit, Some(postgresClient))

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, analyticsClient, Some(ebookSearch))

      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        MockApiKeyRegistry(testKit, authenticator)

      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v2/ebooks?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }
  }

  "/v2/ebooks[id] route" should {
    "return Forbidden if API key not found" in {
      val postgresClient = testKit.spawn(MockPostgresClientKeyNotFound())

      val authenticator: ActorRef[AuthenticationCommand] =
        MockAuthenticator(testKit, Some(postgresClient))

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, analyticsClient, Some(ebookSearch))

      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        MockApiKeyRegistry(testKit, authenticator)

      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v2/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "return Forbidden if API key disabled" in {
      val postgresClient = testKit.spawn(MockPostgresClientDisabled())

      val authenticator: ActorRef[AuthenticationCommand] =
        MockAuthenticator(testKit, Some(postgresClient))

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, analyticsClient, Some(ebookSearch))

      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        MockApiKeyRegistry(testKit, authenticator)

      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v2/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }
  }
}
