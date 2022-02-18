package dpla.ebookapi.v1.ebooks.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.Routes
import dpla.ebookapi.mocks._
import dpla.ebookapi.v1.PostgresClient.PostgresClientCommand
import dpla.ebookapi.v1.apiKey.ApiKeyRegistry
import dpla.ebookapi.v1.apiKey.ApiKeyRegistry.ApiKeyRegistryCommand
import dpla.ebookapi.v1.ebooks.EbookRegistry
import dpla.ebookapi.v1.ebooks.EbookRegistry.EbookRegistryCommand
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.EsClientCommand
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PostgresUnauthorizedTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem
  val elasticSearchClient: ActorRef[EsClientCommand] =
    testKit.spawn(MockEsClientSuccess())

  val apiKey = "08e3918eeb8bf4469924f062072459a8"

  "/v1/ebooks route" should {
    "return Forbidden if API key not found" in {
      val postgresClient: ActorRef[PostgresClientCommand] =
        testKit.spawn(MockPostgresClientUnsuccessful())
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        testKit.spawn(EbookRegistry(elasticSearchClient, postgresClient))
      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        testKit.spawn(ApiKeyRegistry(postgresClient))
      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "return Forbidden if API key disabled" in {
      val postgresClient: ActorRef[PostgresClientCommand] =
        testKit.spawn(MockPostgresClientDisabled())
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        testKit.spawn(EbookRegistry(elasticSearchClient, postgresClient))
      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        testKit.spawn(ApiKeyRegistry(postgresClient))
      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }
  }

  "/v1/ebooks[id] route" should {
    "return Forbidden if API key not found" in {
      val postgresClient: ActorRef[PostgresClientCommand] =
        testKit.spawn(MockPostgresClientUnsuccessful())
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        testKit.spawn(EbookRegistry(elasticSearchClient, postgresClient))
      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        testKit.spawn(ApiKeyRegistry(postgresClient))
      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }

    "return Forbidden if API key disabled" in {
      val postgresClient: ActorRef[PostgresClientCommand] =
        testKit.spawn(MockPostgresClientDisabled())
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        testKit.spawn(EbookRegistry(elasticSearchClient, postgresClient))
      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        testKit.spawn(ApiKeyRegistry(postgresClient))
      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
      }
    }
  }
}