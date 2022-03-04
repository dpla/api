package dpla.ebookapi.v1.ebooks.httpHeadersAndMethods

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.Routes
import dpla.ebookapi.mocks.{MockApiKeyRegistry, MockEbookRegistry, MockEsClientSuccess, MockPostgresClientSuccess}
import dpla.ebookapi.v1.PostgresClient.PostgresClientCommand
import dpla.ebookapi.v1.apiKey.ApiKeyRegistryCommand
import dpla.ebookapi.v1.ebooks.EbookRegistryCommand
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.EsClientCommand
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PermittedMediaTypesTest extends AnyWordSpec with Matchers
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

  val mockEbookRegistry = new MockEbookRegistry(testKit)
  mockEbookRegistry.setAuthenticationClient(postgresClient)
  mockEbookRegistry.setSearchIndexClient(elasticSearchClient)
  val ebookRegistry: ActorRef[EbookRegistryCommand] =
    mockEbookRegistry.getRef

  val mockApiKeyRegistry = new MockApiKeyRegistry(testKit)
  mockApiKeyRegistry.setAuthenticationClient(postgresClient)
  val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
    mockApiKeyRegistry.getRef

  lazy val routes: Route =
    new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

  val apiKey = "08e3918eeb8bf4469924f062072459a8"

  "/v1/ebooks route" should {
    "reject invalid media types" in {
      val request = Get(s"/v1/ebooks?api_key=$apiKey")
        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/xml`))))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.NotAcceptable
      }
    }

    "allow valid media type" in {
      val request = Get(s"/v1/ebooks?api_key=$apiKey")
        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/json`))))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }

  "/v1/ebooks[id] route" should {
    "reject invalid media types" in {
      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$apiKey")
        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/xml`))))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.NotAcceptable
      }
    }

    "allow valid media type" in {
      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$apiKey")
        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/json`))))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }
}
