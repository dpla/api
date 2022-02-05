package dpla.ebookapi.v1.ebooks

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.Routes
import dpla.ebookapi.helpers.FileReader
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.{EsClientCommand, GetEsFetchResult, GetEsSearchResult}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ResponseHeadersTest extends AnyWordSpec with Matchers with ScalatestRouteTest with FileReader {

  object MockElasticSearchClient {

    private val searchBody: String = readFile("/elasticSearchMinimalEbookList.json")
    private val fetchBody: String = readFile("/elasticSearchMinimalEbook.json")

    def apply(): Behavior[ElasticSearchClient.EsClientCommand] = {
      Behaviors.receiveMessage[ElasticSearchClient.EsClientCommand] {

        case GetEsSearchResult(_, replyTo) =>
          replyTo ! EsSuccess(searchBody)
          Behaviors.same

        case GetEsFetchResult(_, replyTo) =>
          replyTo ! EsSuccess(fetchBody)
          Behaviors.same
      }
    }
  }

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()
  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.classicSystem
  val ebookRegistry: ActorRef[EbookRegistry.RegistryCommand] = testKit.spawn(EbookRegistry())
  val elasticSearchClient: ActorRef[EsClientCommand] = testKit.spawn(MockElasticSearchClient())
  lazy val routes: Route = new Routes(ebookRegistry, elasticSearchClient).applicationRoutes

  "/v1/ebooks response header" should {
    "include correct Content-Type" in {
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        contentType.mediaType shouldEqual MediaTypes.`application/json`
      }
    }

    "include correct Content-Security-Policy" in {
      val request = Get("/v1/ebooks")
      val expected = "default-src 'none'; script-src 'self'; frame-ancestors 'none'; form-action 'self'"

      request ~> Route.seal(routes) ~> check {
        header("Content-Security-Policy").get.value shouldEqual expected
      }
    }

    "include correct X-Content-Type-Options" in {
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        header("X-Content-Type-Options").get.value shouldEqual "nosniff"
      }
    }

    "include correct X-Frame-Options" in {
      val request = Get("/v1/ebooks")

      request ~> Route.seal(routes) ~> check {
        header("X-Frame-Options").get.value shouldEqual "DENY"
      }
    }
  }

  "/v1/ebooks[id] response header" should {
    "include correct Content-Type" in {
      val request = Get("/v1/ebooks/R0VfVX4BfY91SSpFGqxt")

      request ~> Route.seal(routes) ~> check {
        contentType.mediaType shouldEqual MediaTypes.`application/json`
      }
    }

    "include correct Content-Security-Policy" in {
      val request = Get("/v1/ebooks/R0VfVX4BfY91SSpFGqxt")
      val expected = "default-src 'none'; script-src 'self'; frame-ancestors 'none'; form-action 'self'"

      request ~> Route.seal(routes) ~> check {
        header("Content-Security-Policy").get.value shouldEqual expected
      }
    }

    "include correct X-Content-Type-Options" in {
      val request = Get("/v1/ebooks/R0VfVX4BfY91SSpFGqxt")

      request ~> Route.seal(routes) ~> check {
        header("X-Content-Type-Options").get.value shouldEqual "nosniff"
      }
    }

    "include correct X-Frame-Options" in {
      val request = Get("/v1/ebooks/R0VfVX4BfY91SSpFGqxt")

      request ~> Route.seal(routes) ~> check {
        header("X-Frame-Options").get.value shouldEqual "DENY"
      }
    }
  }
}

