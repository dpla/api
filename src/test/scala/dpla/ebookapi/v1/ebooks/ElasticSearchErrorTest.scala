package dpla.ebookapi.v1.ebooks

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.Routes
import dpla.ebookapi.mocks.{MockEsClientNotFound, MockEsClientParseError, MockEsClientServerError, MockEsClientUnreachable}
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.EsClientCommand
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ElasticSearchErrorTest extends AnyWordSpec with Matchers with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.classicSystem
  val ebookRegistry: ActorRef[EbookRegistry.RegistryCommand] = testKit.spawn(EbookRegistry())

  "/v1/ebooks route" should {
    "return Teapot if ElasticSearch entity cannot be parsed" in {
      val elasticSearchClient: ActorRef[EsClientCommand] = testKit.spawn(MockEsClientParseError())
      lazy val routes: Route = new Routes(ebookRegistry, elasticSearchClient).applicationRoutes

      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }

    "return Teapot if ElasticSearch returns server error" in {
      val elasticSearchClient: ActorRef[EsClientCommand] = testKit.spawn(MockEsClientServerError())
      lazy val routes: Route = new Routes(ebookRegistry, elasticSearchClient).applicationRoutes

      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }

    "return Teapot if call to ElasticSearch fails" in {
      val elasticSearchClient: ActorRef[EsClientCommand] = testKit.spawn(MockEsClientUnreachable())
      lazy val routes: Route = new Routes(ebookRegistry, elasticSearchClient).applicationRoutes

      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }
  }

  "/v1/ebooks[id] route" should {
    "return Teapot if ElasticSearch entity cannot be parsed" in {
      val elasticSearchClient: ActorRef[EsClientCommand] = testKit.spawn(MockEsClientParseError())
      lazy val routes: Route = new Routes(ebookRegistry, elasticSearchClient).applicationRoutes

      val request = Get("/v1/ebooks/R0VfVX4BfY91SSpFGqxt")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }

    "return Not Found if ebook not found" in {
      val elasticSearchClient: ActorRef[EsClientCommand] = testKit.spawn(MockEsClientNotFound())
      lazy val routes: Route = new Routes(ebookRegistry, elasticSearchClient).applicationRoutes

      val request = Get("/v1/ebooks/R0VfVX4BfY91SSpFGqxt")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "return Teapot if ElasticSearch returns server error" in {
      val elasticSearchClient: ActorRef[EsClientCommand] = testKit.spawn(MockEsClientServerError())
      lazy val routes: Route = new Routes(ebookRegistry, elasticSearchClient).applicationRoutes

      val request = Get("/v1/ebooks/R0VfVX4BfY91SSpFGqxt")
        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/json`))))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }

    "return Teapot if call to ElasticSearch fails" in {
      val elasticSearchClient: ActorRef[EsClientCommand] = testKit.spawn(MockEsClientUnreachable())
      lazy val routes: Route = new Routes(ebookRegistry, elasticSearchClient).applicationRoutes

      val request = Get("/v1/ebooks/R0VfVX4BfY91SSpFGqxt")
        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/json`))))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.ImATeapot
      }
    }
  }
}
