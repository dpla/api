package dpla.ebookapi.v1.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.Routes
import dpla.ebookapi.mocks._
import dpla.ebookapi.v1.EmailClient.EmailClientCommand
import dpla.ebookapi.v1.apiKey.ApiKeyRegistryCommand
import dpla.ebookapi.v1.authentication.AuthProtocol.AuthenticationCommand
import dpla.ebookapi.v1.authentication.{MockAuthenticator, MockPostgresClientError, MockPostgresClientExistingKey}
import dpla.ebookapi.v1.registry.EbookRegistryCommand
import dpla.ebookapi.v1.search.SearchProtocol.SearchCommand
import dpla.ebookapi.v1.search.{EbookMapper, MockEbookSearch, MockEsClientSuccess}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PostgresErrorTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val mapper = testKit.spawn(EbookMapper())
  val elasticSearchClient = testKit.spawn(MockEsClientSuccess(mapper))

  val mockEbookSearch = new MockEbookSearch(testKit)
  mockEbookSearch.setElasticSearchClient(elasticSearchClient)
  mockEbookSearch.setEbookMapper(mapper)
  val ebookSearch: ActorRef[SearchCommand] =
    mockEbookSearch.getRef

  val apiKey = "08e3918eeb8bf4469924f062072459a8"

  "/v1/ebooks route" should {
    "return Teapot if Postgres errors" in {
      val postgresClient = testKit.spawn(MockPostgresClientError())

      val mockAuthenticator = new MockAuthenticator(testKit)
      mockAuthenticator.setPostgresClient(postgresClient)
      val authenticator: ActorRef[AuthenticationCommand] = mockAuthenticator.getRef

      val mockEbookRegistry = new MockEbookRegistry(testKit)
      mockEbookRegistry.setAuthenticator(authenticator)
      mockEbookRegistry.setEbookSearch(ebookSearch)
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        mockEbookRegistry.getRef

      val mockApiKeyRegistry = new MockApiKeyRegistry(testKit)
      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        mockApiKeyRegistry.getRef

      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }
  }

  "/v1/ebooks[id] route" should {
    "return Teapot if Postgres errors" in {
      val postgresClient = testKit.spawn(MockPostgresClientError())

      val mockAuthenticator = new MockAuthenticator(testKit)
      mockAuthenticator.setPostgresClient(postgresClient)
      val authenticator: ActorRef[AuthenticationCommand] = mockAuthenticator.getRef

      val mockEbookRegistry = new MockEbookRegistry(testKit)
      mockEbookRegistry.setAuthenticator(authenticator)
      mockEbookRegistry.setEbookSearch(ebookSearch)
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        mockEbookRegistry.getRef

      val mockApiKeyRegistry = new MockApiKeyRegistry(testKit)
      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        mockApiKeyRegistry.getRef

      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }
  }

  "/api_key/[email]" should {
    "return Teapot if Postgres errors" in {
      val postgresClient = testKit.spawn(MockPostgresClientError())

      val mockEbookRegistry = new MockEbookRegistry(testKit)
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        mockEbookRegistry.getRef

      val mockAuthenticator = new MockAuthenticator(testKit)
      mockAuthenticator.setPostgresClient(postgresClient)
      val authenticator: ActorRef[AuthenticationCommand] = mockAuthenticator.getRef

      val mockApiKeyRegistry = new MockApiKeyRegistry(testKit)
      mockApiKeyRegistry.setAuthenticator(authenticator)
      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        mockApiKeyRegistry.getRef

      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Post(s"/v1/api_key/email@example.com")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }
  }

  "/api_key/[email] route" should {
    "return Conflict if email has existing api key" in {
      val postgresClient = testKit.spawn(MockPostgresClientExistingKey())
      val emailClient: ActorRef[EmailClientCommand] =
        testKit.spawn(MockEmailClientSuccess())

      val mockEbookRegistry = new MockEbookRegistry(testKit)
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        mockEbookRegistry.getRef

      val mockAuthenticator = new MockAuthenticator(testKit)
      mockAuthenticator.setPostgresClient(postgresClient)
      val authenticator: ActorRef[AuthenticationCommand] = mockAuthenticator.getRef

      val mockApiKeyRegistry = new MockApiKeyRegistry(testKit)
      mockApiKeyRegistry.setAuthenticator(authenticator)
      mockApiKeyRegistry.setEmailClient(emailClient)
      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        mockApiKeyRegistry.getRef

      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Post("/v1/api_key/email@example.com")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Conflict
      }
    }
  }
}
