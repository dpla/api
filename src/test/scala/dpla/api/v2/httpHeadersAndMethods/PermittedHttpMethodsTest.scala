package dpla.api.v2.httpHeadersAndMethods

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.v2.analytics.AnalyticsClient
import dpla.api.v2.analytics.AnalyticsClient.AnalyticsClientCommand
import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
import dpla.api.v2.authentication.{MockAuthenticator, MockPostgresClientSuccess}
import dpla.api.v2.registry.{ApiKeyRegistryCommand, MockApiKeyRegistry, MockEbookRegistry, MockItemRegistry, MockPssRegistry, SearchRegistryCommand}
import dpla.api.v2.search.{MockEbookSearch, MockEboookEsClientSuccess}
import dpla.api.v2.search.SearchProtocol.SearchCommand
import dpla.api.v2.search.mappings.DPLAMAPMapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PermittedHttpMethodsTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  implicit def typedSystem: ActorSystem[Nothing] =
    testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val analyticsClient: ActorRef[AnalyticsClientCommand] =
    testKit.spawn(AnalyticsClient())
  val postgresClient = testKit.spawn(MockPostgresClientSuccess())
  val mapper = testKit.spawn(DPLAMAPMapper())
  val elasticSearchClient = testKit.spawn(MockEboookEsClientSuccess(mapper))

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

  val pssRegistry: ActorRef[SearchRegistryCommand] =
    MockPssRegistry(testKit, authenticator, analyticsClient)

  lazy val routes: Route =
    new Routes(ebookRegistry, itemRegistry, pssRegistry, apiKeyRegistry)
      .applicationRoutes

  "/v2/ebooks route" should {
    "handle invalid HTTP methods" should {
      "reject DELETE" in {
        val request = Delete("/v2/ebooks")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject PATCH" in {
        val request = Patch("/v2/ebooks")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject POST" in {
        val request = Post("/v2/ebooks")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject PUT" in {
        val request = Put("/v2/ebooks")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }
    }
  }

  "/v2/ebooks/[id] route" should {
    "handle invalid HTTP methods" should {
      "reject DELETE" in {
        val request = Delete("/v2/ebooks/R0VfVX4BfY91SSpFGqxt")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject PATCH" in {
        val request = Patch("/v2/ebooks/R0VfVX4BfY91SSpFGqxt")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject POST" in {
        val request = Post("/v2/ebooks/R0VfVX4BfY91SSpFGqxt")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject PUT" in {
        val request = Put("/v2/ebooks/R0VfVX4BfY91SSpFGqxt")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }
    }
  }

  "/v2/api_key/[email] route" should {
    "handle invalid HTTP methods" should {
      "reject GET" in {
        val request = Get("/v2/api_key/email@example.com")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject DELETE" in {
        val request = Delete("/v2/api_key/email@example.com")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject PATCH" in {
        val request = Patch("/v2/api_key/email@example.com")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject PUT" in {
        val request = Put("/v2/api_key/email@example.com")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }
    }
  }
}
