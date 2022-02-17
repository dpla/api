package dpla.ebookapi.v1.ebooks.httpHeadersAndMethods

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.Routes
import dpla.ebookapi.helpers.FileReader
import dpla.ebookapi.mocks.{MockEsClientSuccess, MockPostgresClientSuccess}
import dpla.ebookapi.v1.PostgresClient.PostgresClientCommand
import dpla.ebookapi.v1.apiKey.ApiKeyRegistry
import dpla.ebookapi.v1.apiKey.ApiKeyRegistry.ApiKeyRegistryCommand
import dpla.ebookapi.v1.ebooks.EbookRegistry
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
  val ebookRegistry: ActorRef[EbookRegistry.EbookRegistryCommand] =
    testKit.spawn(EbookRegistry(elasticSearchClient, postgresClient))
  val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
    testKit.spawn(ApiKeyRegistry(postgresClient))
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

