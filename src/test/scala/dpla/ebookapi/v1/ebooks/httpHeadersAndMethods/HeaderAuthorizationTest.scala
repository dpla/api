package dpla.ebookapi.v1.ebooks.httpHeadersAndMethods

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.Routes
import dpla.ebookapi.mocks.{MockEsClientSuccess, MockPostgresClientSuccess}
import dpla.ebookapi.v1.PostgresClient.PostgresClientCommand
import dpla.ebookapi.v1.apiKey.ApiKeyRegistry
import dpla.ebookapi.v1.apiKey.ApiKeyRegistry.ApiKeyRegistryCommand
import dpla.ebookapi.v1.ebooks.EbookRegistry
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.EsClientCommand
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HeaderAuthorizationTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
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

  "/v1/ebooks route" should {
    "accept API key in HTTP header" in {
      val request = Get(s"/v1/ebooks")
        .withHeaders(RawHeader("Authorization", apiKey))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "privilege API key in HTTP header over that in query" in {
      val request = Get(s"/v1/ebooks?api_key=foo")
        .withHeaders(RawHeader("Authorization", apiKey))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "fail if API key is neither in HTTP header nor query" in {
      val request = Get(s"/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }
  }

  "/v1/ebooks[id] route" should {
    "accept API key in HTTP header" in {
      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt")
        .withHeaders(RawHeader("Authorization", apiKey))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "privilege API key in HTTP header over that in query" in {
      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=foo")
        .withHeaders(RawHeader("Authorization", apiKey))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "fail if API key is neither in HTTP header nor query" in {
      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }
  }
}
