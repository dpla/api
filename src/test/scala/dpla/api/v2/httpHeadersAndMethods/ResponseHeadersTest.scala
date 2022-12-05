package dpla.api.v2.httpHeadersAndMethods

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.helpers.{ActorHelper, FileReader}
import dpla.api.helpers.Utils.fakeApiKey
import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
import dpla.api.v2.authentication.{MockAuthenticator, MockPostgresClientSuccess}
import dpla.api.v2.registry.{ApiKeyRegistryCommand, MockApiKeyRegistry, MockEbookRegistry, MockItemRegistry, MockPssRegistry, SearchRegistryCommand}
import dpla.api.v2.search.{MockEbookSearch, MockEboookEsClientSuccess}
import dpla.api.v2.search.SearchProtocol.SearchCommand
import dpla.api.v2.search.mappings.DPLAMAPMapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ResponseHeadersTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest with FileReader with ActorHelper {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  implicit def typedSystem: ActorSystem[Nothing] =
    testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val postgresClient = testKit.spawn(MockPostgresClientSuccess())
  val mapper = testKit.spawn(DPLAMAPMapper())
  val elasticSearchClient = testKit.spawn(MockEboookEsClientSuccess(mapper))

  val ebookSearch: ActorRef[SearchCommand] =
    MockEbookSearch(testKit, Some(elasticSearchClient), Some(mapper))

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

  "/v2/ebooks response header" should {
    "include correct Content-Type" in {
      val request = Get(s"/v2/ebooks?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        contentType.mediaType shouldEqual MediaTypes.`application/json`
      }
    }

    "include correct Content-Security-Policy" in {
      val request = Get(s"/v2/ebooks?api_key=$fakeApiKey")
      val expected =
        "default-src 'none'; script-src 'self'; frame-ancestors 'none'; form-action 'self'"

      request ~> Route.seal(routes) ~> check {
        header("Content-Security-Policy").get.value shouldEqual expected
      }
    }

    "include correct X-Content-Type-Options" in {
      val request = Get(s"/v2/ebooks?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        header("X-Content-Type-Options").get.value shouldEqual "nosniff"
      }
    }

    "include correct X-Frame-Options" in {
      val request = Get(s"/v2/ebooks?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        header("X-Frame-Options").get.value shouldEqual "DENY"
      }
    }
  }

  "/v2/ebooks[id] response header" should {
    "include correct Content-Type" in {
      val request = Get(s"/v2/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        contentType.mediaType shouldEqual MediaTypes.`application/json`
      }
    }

    "include correct Content-Security-Policy" in {
      val request = Get(s"/v2/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$fakeApiKey")
      val expected =
        "default-src 'none'; script-src 'self'; frame-ancestors 'none'; form-action 'self'"

      request ~> Route.seal(routes) ~> check {
        header("Content-Security-Policy").get.value shouldEqual expected
      }
    }

    "include correct X-Content-Type-Options" in {
      val request = Get(s"/v2/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        header("X-Content-Type-Options").get.value shouldEqual "nosniff"
      }
    }

    "include correct X-Frame-Options" in {
      val request = Get(s"/v2/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        header("X-Frame-Options").get.value shouldEqual "DENY"
      }
    }
  }
}

