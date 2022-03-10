package dpla.ebookapi.v1.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.Routes
import dpla.ebookapi.v1.analytics.AnalyticsClient
import dpla.ebookapi.v1.analytics.AnalyticsClient.AnalyticsClientCommand
import dpla.ebookapi.v1.authentication.AuthProtocol.AuthenticationCommand
import dpla.ebookapi.v1.authentication.{MockAuthenticator, MockPostgresClientSuccess}
import dpla.ebookapi.v1.registry.{ApiKeyRegistryCommand, EbookRegistryCommand, MockApiKeyRegistry, MockEbookRegistry}
import dpla.ebookapi.v1.search.{MockEbookSearch, MockEsClientFailure, MockEsClientNotFound}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ElasticSearchErrorTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val analyticsClient: ActorRef[AnalyticsClientCommand] =
    testKit.spawn(AnalyticsClient())

  val postgresClient = testKit.spawn(MockPostgresClientSuccess())

  val mockAuthenticator = new MockAuthenticator(testKit)
  mockAuthenticator.setPostgresClient(postgresClient)
  val authenticator: ActorRef[AuthenticationCommand] = mockAuthenticator.getRef

  val mockApiKeyRegistry = new MockApiKeyRegistry(testKit, authenticator)
  val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
    mockApiKeyRegistry.getRef

  val apiKey = "08e3918eeb8bf4469924f062072459a8"

  "/v1/ebooks route" should {
    "return Teapot if ElasticSearch entity cannot be parsed" in {
      val elasticSearchClient = testKit.spawn(MockEsClientFailure())

      val mockEbookSearch = new MockEbookSearch(testKit)
      mockEbookSearch.setElasticSearchClient(elasticSearchClient)
      val ebookSearch = mockEbookSearch.getRef

      val mockEbookRegistry = new MockEbookRegistry(testKit, authenticator, analyticsClient)
      mockEbookRegistry.setEbookSearch(ebookSearch)
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        mockEbookRegistry.getRef

      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }

    "return Teapot if call to ElasticSearch fails" in {
      val elasticSearchClient = testKit.spawn(MockEsClientFailure())

      val mockEbookSearch = new MockEbookSearch(testKit)
      mockEbookSearch.setElasticSearchClient(elasticSearchClient)
      val ebookSearch = mockEbookSearch.getRef

      val mockEbookRegistry = new MockEbookRegistry(testKit, authenticator, analyticsClient)
      mockEbookRegistry.setEbookSearch(ebookSearch)
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        mockEbookRegistry.getRef

      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }
  }

  "/v1/ebooks[id] route" should {

    "return Not Found if ebook not found" in {
      val elasticSearchClient = testKit.spawn(MockEsClientNotFound())

      val mockEbookSearch = new MockEbookSearch(testKit)
      mockEbookSearch.setElasticSearchClient(elasticSearchClient)
      val ebookSearch = mockEbookSearch.getRef

      val mockEbookRegistry = new MockEbookRegistry(testKit, authenticator, analyticsClient)
      mockEbookRegistry.setEbookSearch(ebookSearch)
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        mockEbookRegistry.getRef
      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "return Teapot if call to ElasticSearch fails" in {
      val elasticSearchClient = testKit.spawn(MockEsClientFailure())

      val mockEbookSearch = new MockEbookSearch(testKit)
      mockEbookSearch.setElasticSearchClient(elasticSearchClient)
      val ebookSearch = mockEbookSearch.getRef

      val mockEbookRegistry = new MockEbookRegistry(testKit, authenticator, analyticsClient)
      mockEbookRegistry.setEbookSearch(ebookSearch)
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        mockEbookRegistry.getRef

      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$apiKey")
        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/json`))))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }
  }
}
