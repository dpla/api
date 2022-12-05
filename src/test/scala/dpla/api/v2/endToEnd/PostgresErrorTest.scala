package dpla.api.v2.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.helpers.ActorHelper
import dpla.api.helpers.Utils.fakeApiKey
import dpla.api.v2.registry.{ApiKeyRegistryCommand, MockApiKeyRegistry, MockEbookRegistry, MockItemRegistry, MockPssRegistry, SearchRegistryCommand}
import dpla.api.v2.search.SearchProtocol.SearchCommand
import dpla.api.v2.search.MockEbookSearch
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PostgresErrorTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest with ActorHelper {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val apiKeyRegistryAuthError: ActorRef[ApiKeyRegistryCommand] =
    MockApiKeyRegistry(testKit, authenticatorError)

  val apiKeyRegistryExistingKey: ActorRef[ApiKeyRegistryCommand] =
    MockApiKeyRegistry(testKit, authenticatorExistingKey, Some(emailClient))

  val itemRegistryAuthError: ActorRef[SearchRegistryCommand] =
    MockItemRegistry(testKit, authenticatorError, itemAnalyticsClient)

  val itemRegistryExistingKey: ActorRef[SearchRegistryCommand] =
    MockItemRegistry(testKit, authenticatorExistingKey, itemAnalyticsClient)

  val ebookSearch: ActorRef[SearchCommand] =
    MockEbookSearch(testKit, Some(ebookElasticSearchClient), Some(dplaMapMapper))

  "/v2/ebooks route" should {
    "return InternalServerError if Postgres errors" in {

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticatorError, ebookAnalyticsClient, Some(ebookSearch))

      val pssRegistry: ActorRef[SearchRegistryCommand] =
        MockPssRegistry(testKit, authenticatorError, pssAnalyticsClient)

      lazy val routes: Route =
        new Routes(ebookRegistry, itemRegistryAuthError, pssRegistry, apiKeyRegistry)
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

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticatorError, ebookAnalyticsClient, Some(ebookSearch))

      val pssRegistry: ActorRef[SearchRegistryCommand] =
        MockPssRegistry(testKit, authenticatorError, pssAnalyticsClient)

      lazy val routes: Route =
        new Routes(ebookRegistry, itemRegistryAuthError, pssRegistry, apiKeyRegistryAuthError)
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

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticatorError, ebookAnalyticsClient)

      val pssRegistry: ActorRef[SearchRegistryCommand] =
        MockPssRegistry(testKit, authenticatorError, pssAnalyticsClient)

      lazy val routes: Route =
        new Routes(ebookRegistry, itemRegistryAuthError, pssRegistry, apiKeyRegistryAuthError)
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

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticatorExistingKey, ebookAnalyticsClient)

      val pssRegistry: ActorRef[SearchRegistryCommand] =
        MockPssRegistry(testKit, authenticatorExistingKey, pssAnalyticsClient)

      lazy val routes: Route =
        new Routes(ebookRegistry, itemRegistryExistingKey, pssRegistry, apiKeyRegistryExistingKey)
          .applicationRoutes

      val request = Post("/v2/api_key/email@example.com")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Conflict
        contentType should === (ContentTypes.`application/json`)
      }
    }
  }
}
