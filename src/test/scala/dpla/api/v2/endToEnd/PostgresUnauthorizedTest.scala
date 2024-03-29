package dpla.api.v2.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.helpers.ActorHelper
import dpla.api.helpers.Utils.fakeApiKey
import dpla.api.v2.registry.{ApiKeyRegistryCommand, MockApiKeyRegistry, MockEbookRegistry, SearchRegistryCommand}
import dpla.api.v2.search.SearchProtocol.SearchCommand
import dpla.api.v2.search.MockEbookSearch
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PostgresUnauthorizedTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest with ActorHelper {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val apiKeyRegistryKeyNotFound: ActorRef[ApiKeyRegistryCommand] =
    MockApiKeyRegistry(testKit, authenticatorKeyNotFound)

  val apiKeyRegistryDisabled: ActorRef[ApiKeyRegistryCommand] =
    MockApiKeyRegistry(testKit, authenticatorDisabled)

  val ebookSearch: ActorRef[SearchCommand] =
    MockEbookSearch(testKit, Some(ebookElasticSearchClient), Some(dplaMapMapper))

  "/v2/ebooks route" should {
    "return Forbidden if API key not found" in {

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticatorKeyNotFound, ebookAnalyticsClient, Some(ebookSearch))

      lazy val routes: Route =
        new Routes(ebookRegistry, itemRegistry, pssRegistry,
          apiKeyRegistryKeyNotFound, smrRegistry).applicationRoutes

      val request = Get(s"/v2/ebooks?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
        contentType should === (ContentTypes.`application/json`)
      }
    }

    "return Forbidden if API key disabled" in {

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticatorDisabled, ebookAnalyticsClient, Some(ebookSearch))

      lazy val routes: Route =
        new Routes(ebookRegistry, itemRegistry, pssRegistry,
          apiKeyRegistryDisabled, smrRegistry).applicationRoutes

      val request = Get(s"/v2/ebooks?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
        contentType should === (ContentTypes.`application/json`)
      }
    }
  }

  "/v2/ebooks[id] route" should {
    "return Forbidden if API key not found" in {

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticatorKeyNotFound, ebookAnalyticsClient, Some(ebookSearch))

      lazy val routes: Route =
        new Routes(ebookRegistry, itemRegistry, pssRegistry,
          apiKeyRegistryKeyNotFound, smrRegistry).applicationRoutes

      val request = Get(s"/v2/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
        contentType should === (ContentTypes.`application/json`)
      }
    }

    "return Forbidden if API key disabled" in {

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticatorDisabled, ebookAnalyticsClient, Some(ebookSearch))

      lazy val routes: Route =
        new Routes(ebookRegistry, itemRegistry, pssRegistry,
          apiKeyRegistryDisabled, smrRegistry).applicationRoutes

      val request = Get(s"/v2/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.Forbidden
        contentType should === (ContentTypes.`application/json`)
      }
    }
  }
}
