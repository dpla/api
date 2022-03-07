package dpla.ebookapi.v1.ebooks.httpHeadersAndMethods

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.Routes
import dpla.ebookapi.helpers.FileReader
import dpla.ebookapi.mocks.{MockApiKeyRegistry, MockAuthenticator, MockEbookRegistry, MockEsClientSuccess, MockPostgresClientSuccess}
import dpla.ebookapi.v1.authentication.PostgresClient.PostgresClientCommand
import dpla.ebookapi.v1.apiKey.ApiKeyRegistryCommand
import dpla.ebookapi.v1.authentication.AuthenticatorCommand
import dpla.ebookapi.v1.ebooks.EbookRegistryCommand
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.EsClientCommand
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ResponseHeadersTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest with FileReader {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  implicit def typedSystem: ActorSystem[Nothing] =
    testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem
  val postgresClient: ActorRef[PostgresClientCommand] =
    testKit.spawn(MockPostgresClientSuccess())
  val elasticSearchClient: ActorRef[EsClientCommand] =
    testKit.spawn(MockEsClientSuccess())

  val mockAuthenticator = new MockAuthenticator(testKit)
  mockAuthenticator.setPostgresClient(postgresClient)
  val authenticator: ActorRef[AuthenticatorCommand] = mockAuthenticator.getRef

  val mockEbookRegistry = new MockEbookRegistry(testKit)
  mockEbookRegistry.setAuthenticator(authenticator)
  mockEbookRegistry.setSearchIndexClient(elasticSearchClient)
  val ebookRegistry: ActorRef[EbookRegistryCommand] =
    mockEbookRegistry.getRef

  val mockApiKeyRegistry = new MockApiKeyRegistry(testKit)
  mockApiKeyRegistry.setAuthenticator(authenticator)
  val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
    mockApiKeyRegistry.getRef

  lazy val routes: Route =
    new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

  val apiKey = "08e3918eeb8bf4469924f062072459a8"

  "/v1/ebooks response header" should {
    "include correct Content-Type" in {
      val request = Get(s"/v1/ebooks?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        contentType.mediaType shouldEqual MediaTypes.`application/json`
      }
    }

    "include correct Content-Security-Policy" in {
      val request = Get(s"/v1/ebooks?api_key=$apiKey")
      val expected =
        "default-src 'none'; script-src 'self'; frame-ancestors 'none'; form-action 'self'"

      request ~> Route.seal(routes) ~> check {
        header("Content-Security-Policy").get.value shouldEqual expected
      }
    }

    "include correct X-Content-Type-Options" in {
      val request = Get(s"/v1/ebooks?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        header("X-Content-Type-Options").get.value shouldEqual "nosniff"
      }
    }

    "include correct X-Frame-Options" in {
      val request = Get(s"/v1/ebooks?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        header("X-Frame-Options").get.value shouldEqual "DENY"
      }
    }
  }

  "/v1/ebooks[id] response header" should {
    "include correct Content-Type" in {
      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        contentType.mediaType shouldEqual MediaTypes.`application/json`
      }
    }

    "include correct Content-Security-Policy" in {
      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$apiKey")
      val expected =
        "default-src 'none'; script-src 'self'; frame-ancestors 'none'; form-action 'self'"

      request ~> Route.seal(routes) ~> check {
        header("Content-Security-Policy").get.value shouldEqual expected
      }
    }

    "include correct X-Content-Type-Options" in {
      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        header("X-Content-Type-Options").get.value shouldEqual "nosniff"
      }
    }

    "include correct X-Frame-Options" in {
      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        header("X-Frame-Options").get.value shouldEqual "DENY"
      }
    }
  }
}

