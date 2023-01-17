package dpla.api.v2.httpHeadersAndMethods

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.helpers.ActorHelper
import dpla.api.v2.registry.{MockEbookRegistry, SearchRegistryCommand}
import dpla.api.v2.search.MockEbookSearch
import dpla.api.v2.search.SearchProtocol.SearchCommand
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PermittedHttpMethodsTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest with ActorHelper {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  implicit def typedSystem: ActorSystem[Nothing] =
    testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val ebookSearch: ActorRef[SearchCommand] =
    MockEbookSearch(testKit, Some(ebookElasticSearchClient), Some(dplaMapMapper))

  val ebookRegistry: ActorRef[SearchRegistryCommand] =
    MockEbookRegistry(testKit, authenticator, ebookAnalyticsClient, Some(ebookSearch))

  lazy val routes: Route =
    new Routes(ebookRegistry, itemRegistry, pssRegistry, apiKeyRegistry,
      smrRegistry).applicationRoutes

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
