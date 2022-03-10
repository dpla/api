package dpla.ebookapi.v1.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.Routes
import dpla.ebookapi.v1.authentication.AuthProtocol.AuthenticationCommand
import dpla.ebookapi.v1.authentication.{MockAuthenticator, MockPostgresClientSuccess}
import dpla.ebookapi.v1.registry.{ApiKeyRegistryCommand, EbookRegistryCommand, MockApiKeyRegistry, MockEbookRegistry}
import dpla.ebookapi.v1.search.MockEbookSearch
import dpla.ebookapi.v1.search.SearchProtocol.SearchCommand
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

  val postgresClient = testKit.spawn(MockPostgresClientSuccess())

  val mockAuthenticator = new MockAuthenticator(testKit)
  mockAuthenticator.setPostgresClient(postgresClient)
  val authenticator: ActorRef[AuthenticationCommand] = mockAuthenticator.getRef

  val mockEbookSearch = new MockEbookSearch(testKit)
  val ebookSearch: ActorRef[SearchCommand] =
    mockEbookSearch.getRef

  val mockEbookRegistry = new MockEbookRegistry(testKit)
  mockEbookRegistry.setEbookSearch(ebookSearch)
  mockEbookRegistry.setAuthenticator(authenticator)
  val ebookRegistry: ActorRef[EbookRegistryCommand] =
    mockEbookRegistry.getRef

  val mockApiKeyRegistry = new MockApiKeyRegistry(testKit)
  mockApiKeyRegistry.setAuthenticator(authenticator)
  val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
    mockApiKeyRegistry.getRef

  lazy val routes: Route =
    new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

  val apiKey = "08e3918eeb8bf4469924f062072459a8"

  "/v1/ebooks route" should {
    "return BadRequest if params are invalid" in {
      val request = Get(s"/v1/ebooks?page=foo&api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "/v1/ebooks[id] route" should {
    "return BadRequest if id is invalid" in {
      val request = Get(s"/v1/ebooks/<foo>?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "return BadRequest if params are invalid" in {
      val request =
        Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?foo=bar&api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "/api_key/[email]" should {
    "return BadRequest if email is invalid" in {
      val request = Post("/v1/api_key/foo")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }
}
