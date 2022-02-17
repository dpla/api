package dpla.ebookapi.v1.ebooks.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
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

class ElasticSearchErrorTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem
  val postgresClient: ActorRef[PostgresClientCommand] =
    testKit.spawn(MockPostgresClientSuccess())
  val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
    testKit.spawn(ApiKeyRegistry(postgresClient))

  val apiKey = "08e3918eeb8bf4469924f062072459a8"

  "/v1/ebooks route" should {
    "return Teapot if ElasticSearch entity cannot be parsed" in {
      val elasticSearchClient: ActorRef[EsClientCommand] =
        testKit.spawn(MockEsClientParseError())
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        testKit.spawn(EbookRegistry(elasticSearchClient, postgresClient))
      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }

    "return Teapot if ElasticSearch returns server error" in {
      val elasticSearchClient: ActorRef[EsClientCommand] =
        testKit.spawn(MockEsClientServerError())
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        testKit.spawn(EbookRegistry(elasticSearchClient, postgresClient))
      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }

    "return Teapot if call to ElasticSearch fails" in {
      val elasticSearchClient: ActorRef[EsClientCommand] =
        testKit.spawn(MockEsClientUnreachable())
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        testKit.spawn(EbookRegistry(elasticSearchClient, postgresClient))
      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }

    "return Teapot if ElasticSearch response cannot be mapped" in {
      val elasticSearchClient: ActorRef[EsClientCommand] =
        testKit.spawn(MockEsClientUnmappable())
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        testKit.spawn(EbookRegistry(elasticSearchClient, postgresClient))
      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks?api_key=$apiKey")
        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/json`))))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }
  }

  "/v1/ebooks[id] route" should {
    "return Teapot if ElasticSearch entity cannot be parsed" in {
      val elasticSearchClient: ActorRef[EsClientCommand] =
        testKit.spawn(MockEsClientParseError())
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        testKit.spawn(EbookRegistry(elasticSearchClient, postgresClient))
      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }

    "return Not Found if ebook not found" in {
      val elasticSearchClient: ActorRef[EsClientCommand] =
        testKit.spawn(MockEsClientNotFound())
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        testKit.spawn(EbookRegistry(elasticSearchClient, postgresClient))
      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$apiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "return Teapot if ElasticSearch returns server error" in {
      val elasticSearchClient: ActorRef[EsClientCommand] =
        testKit.spawn(MockEsClientServerError())
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        testKit.spawn(EbookRegistry(elasticSearchClient, postgresClient))
      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$apiKey")
        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/json`))))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }

    "return Teapot if call to ElasticSearch fails" in {
      val elasticSearchClient: ActorRef[EsClientCommand] =
        testKit.spawn(MockEsClientUnreachable())
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        testKit.spawn(EbookRegistry(elasticSearchClient, postgresClient))
      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$apiKey")
        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/json`))))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }

    "return Teapot if ElasticSearch response cannot be mapped" in {
      val elasticSearchClient: ActorRef[EsClientCommand] =
        testKit.spawn(MockEsClientUnmappable())
      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        testKit.spawn(EbookRegistry(elasticSearchClient, postgresClient))
      lazy val routes: Route =
        new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v1/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$apiKey")
        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/json`))))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }
  }
}
