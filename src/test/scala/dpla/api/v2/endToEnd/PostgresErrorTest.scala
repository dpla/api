package dpla.api.v2.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.helpers.ActorHelper
import dpla.api.helpers.Utils.fakeApiKey
import dpla.api.v2.email.EmailClient.EmailClientCommand
import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
import dpla.api.v2.authentication.{MockAuthenticator, MockPostgresClientError, MockPostgresClientExistingKey}
import dpla.api.v2.email.MockEmailClientSuccess
import dpla.api.v2.registry.{ApiKeyRegistryCommand, MockApiKeyRegistry, MockEbookRegistry, MockItemRegistry, MockPssRegistry, SearchRegistryCommand}
import dpla.api.v2.search.SearchProtocol.SearchCommand
import dpla.api.v2.search.mappings.DPLAMAPMapper
import dpla.api.v2.search.{MockEbookSearch, MockEboookEsClientSuccess, MockItemSearch}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PostgresErrorTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest with ActorHelper {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val mapper = testKit.spawn(DPLAMAPMapper())
  val elasticSearchClient = testKit.spawn(MockEboookEsClientSuccess(mapper))

  val ebookSearch: ActorRef[SearchCommand] =
    MockEbookSearch(testKit, Some(elasticSearchClient), Some(mapper))

  "/v2/ebooks route" should {
    "return InternalServerError if Postgres errors" in {
      val postgresClient = testKit.spawn(MockPostgresClientError())

      val authenticator: ActorRef[AuthenticationCommand] =
        MockAuthenticator(testKit, Some(postgresClient))

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, ebookAnalyticsClient, Some(ebookSearch))

      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        MockApiKeyRegistry(testKit, authenticator)

      val itemRegistry: ActorRef[SearchRegistryCommand] =
        MockItemRegistry(testKit, authenticator, itemAnalyticsClient)

      val pssRegistry: ActorRef[SearchRegistryCommand] =
        MockPssRegistry(testKit, authenticator, pssAnalyticsClient)

      lazy val routes: Route =
        new Routes(ebookRegistry, itemRegistry, pssRegistry, apiKeyRegistry)
          .applicationRoutes

      val request = Get(s"/v2/ebooks?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.InternalServerError
        contentType should === (ContentTypes.`application/json`)
      }
    }
  }

  "/v2/ebooks[id] route" should {
    "return InternalServerError if Postgres errors" in {
      val postgresClient = testKit.spawn(MockPostgresClientError())

      val authenticator: ActorRef[AuthenticationCommand] =
        MockAuthenticator(testKit, Some(postgresClient))

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, ebookAnalyticsClient, Some(ebookSearch))

      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        MockApiKeyRegistry(testKit, authenticator)

      val itemRegistry: ActorRef[SearchRegistryCommand] =
        MockItemRegistry(testKit, authenticator, itemAnalyticsClient)

      val pssRegistry: ActorRef[SearchRegistryCommand] =
        MockPssRegistry(testKit, authenticator, pssAnalyticsClient)

      lazy val routes: Route =
        new Routes(ebookRegistry, itemRegistry, pssRegistry, apiKeyRegistry)
          .applicationRoutes

      val request = Get(s"/v2/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.InternalServerError
        contentType should === (ContentTypes.`application/json`)
      }
    }
  }

  "/api_key/[email]" should {
    "return InternalServerError if Postgres errors" in {
      val postgresClient = testKit.spawn(MockPostgresClientError())

      val authenticator: ActorRef[AuthenticationCommand] =
        MockAuthenticator(testKit, Some(postgresClient))

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, ebookAnalyticsClient)

      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        MockApiKeyRegistry(testKit, authenticator)

      val itemRegistry: ActorRef[SearchRegistryCommand] =
        MockItemRegistry(testKit, authenticator, itemAnalyticsClient)

      val pssRegistry: ActorRef[SearchRegistryCommand] =
        MockPssRegistry(testKit, authenticator, pssAnalyticsClient)

      lazy val routes: Route =
        new Routes(ebookRegistry, itemRegistry, pssRegistry, apiKeyRegistry)
          .applicationRoutes

      val request = Post(s"/v2/api_key/email@example.com")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.InternalServerError
        contentType should === (ContentTypes.`application/json`)
      }
    }
  }

  "/api_key/[email] route" should {
    "return Conflict if email has existing api key" in {
      val postgresClient = testKit.spawn(MockPostgresClientExistingKey())
      val emailClient: ActorRef[EmailClientCommand] =
        testKit.spawn(MockEmailClientSuccess())

      val authenticator: ActorRef[AuthenticationCommand] =
        MockAuthenticator(testKit, Some(postgresClient))

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, ebookAnalyticsClient)

      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        MockApiKeyRegistry(testKit, authenticator, Some(emailClient))

      val itemRegistry: ActorRef[SearchRegistryCommand] =
        MockItemRegistry(testKit, authenticator, itemAnalyticsClient)

      val pssRegistry: ActorRef[SearchRegistryCommand] =
        MockPssRegistry(testKit, authenticator, pssAnalyticsClient)

      lazy val routes: Route =
        new Routes(ebookRegistry, itemRegistry, pssRegistry, apiKeyRegistry)
          .applicationRoutes

      val request = Post("/v2/api_key/email@example.com")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Conflict
        contentType should === (ContentTypes.`application/json`)
      }
    }
  }
}
