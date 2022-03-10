package dpla.ebookapi.v1.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.Routes
import dpla.ebookapi.v1.analytics.AnalyticsClient
import dpla.ebookapi.v1.analytics.AnalyticsClient.AnalyticsClientCommand
import dpla.ebookapi.v1.authentication.AuthProtocol.AuthenticationCommand
import dpla.ebookapi.v1.authentication.{MockAuthenticator, MockPostgresClientDisabled, MockPostgresClientKeyNotFound}
import dpla.ebookapi.v1.registry.{ApiKeyRegistryCommand, EbookRegistryCommand, MockApiKeyRegistry, MockEbookRegistry}
import dpla.ebookapi.v1.search.SearchProtocol.SearchCommand
import dpla.ebookapi.v1.search.{EbookMapper, MockEbookSearch, MockEsClientSuccess}
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
  val mapper = testKit.spawn(EbookMapper())
  val elasticSearchClient = testKit.spawn(MockEsClientSuccess(mapper))

  val mockEbookSearch = new MockEbookSearch(testKit)
  mockEbookSearch.setElasticSearchClient(elasticSearchClient)
  mockEbookSearch.setEbookMapper(mapper)
  val ebookSearch: ActorRef[SearchCommand] = mockEbookSearch.getRef

  val apiKey = "08e3918eeb8bf4469924f062072459a8"

  "/v1/ebooks route" should {
    "return Forbidden if API key not found" in {
      val postgresClient = testKit.spawn(MockPostgresClientKeyNotFound())

      val mockAuthenticator = new MockAuthenticator(testKit)
      mockAuthenticator.setPostgresClient(postgresClient)
      val authenticator: ActorRef[AuthenticationCommand] = mockAuthenticator.getRef

      val mockEbookRegistry = new MockEbookRegistry(testKit, authenticator, analyticsClient)
      mockEbookRegistry.setEbookSearch(ebookSearch)
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        mockEbookRegistry.getRef

      val mockApiKeyRegistry = new MockApiKeyRegistry(testKit, authenticator)
      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        mockApiKeyRegistry.getRef

      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "return Forbidden if API key disabled" in {
      val postgresClient = testKit.spawn(MockPostgresClientDisabled())

      val mockAuthenticator = new MockAuthenticator(testKit)
      mockAuthenticator.setPostgresClient(postgresClient)
      val authenticator: ActorRef[AuthenticationCommand] = mockAuthenticator.getRef

      val mockEbookRegistry = new MockEbookRegistry(testKit, authenticator, analyticsClient)
      mockEbookRegistry.setEbookSearch(ebookSearch)
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        mockEbookRegistry.getRef

      val mockApiKeyRegistry = new MockApiKeyRegistry(testKit, authenticator)
      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        mockApiKeyRegistry.getRef

      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }
  }

  "/v1/ebooks[id] route" should {
    "return Forbidden if API key not found" in {
      val postgresClient = testKit.spawn(MockPostgresClientKeyNotFound())

      val mockAuthenticator = new MockAuthenticator(testKit)
      mockAuthenticator.setPostgresClient(postgresClient)
      val authenticator: ActorRef[AuthenticationCommand] = mockAuthenticator.getRef

      val mockEbookRegistry = new MockEbookRegistry(testKit, authenticator, analyticsClient)
      mockEbookRegistry.setEbookSearch(ebookSearch)
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        mockEbookRegistry.getRef

      val mockApiKeyRegistry = new MockApiKeyRegistry(testKit, authenticator)
      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        mockApiKeyRegistry.getRef

      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "return Forbidden if API key disabled" in {
      val postgresClient = testKit.spawn(MockPostgresClientDisabled())

      val mockAuthenticator = new MockAuthenticator(testKit)
      mockAuthenticator.setPostgresClient(postgresClient)
      val authenticator: ActorRef[AuthenticationCommand] = mockAuthenticator.getRef

      val mockEbookRegistry = new MockEbookRegistry(testKit, authenticator, analyticsClient)
      mockEbookRegistry.setEbookSearch(ebookSearch)
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        mockEbookRegistry.getRef

      val mockApiKeyRegistry = new MockApiKeyRegistry(testKit, authenticator)
      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        mockApiKeyRegistry.getRef

      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }
  }
}
