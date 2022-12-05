package dpla.api.v2.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.helpers.ActorHelper
import dpla.api.helpers.Utils.fakeApiKey
import dpla.api.v2.registry.{MockEbookRegistry, MockItemRegistry, MockPssRegistry, SearchRegistryCommand}
import dpla.api.v2.search.SearchProtocol.SearchCommand
import dpla.api.v2.search.MockEbookSearch
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MapperFailureTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest with ActorHelper {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val itemRegistry: ActorRef[SearchRegistryCommand] =
    MockItemRegistry(testKit, authenticator, itemAnalyticsClient)

  val pssRegistry: ActorRef[SearchRegistryCommand] =
    MockPssRegistry(testKit, authenticator, pssAnalyticsClient)

  "/v2/ebooks route" should {

    "return InternalServerError if ElasticSearch response cannot be mapped" in {


      val ebookSearch: ActorRef[SearchCommand] =
        MockEbookSearch(testKit, None, Some(mapperFailure))

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, ebookAnalyticsClient, Some(ebookSearch))

      lazy val routes: Route =
        new Routes(ebookRegistry, itemRegistry, pssRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v2/ebooks?api_key=$fakeApiKey")
        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/json`))))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.InternalServerError
        contentType should === (ContentTypes.`application/json`)
      }
    }
  }

  "/v2/ebooks[id] route" should {

    "return InternalServerError if ElasticSearch response cannot be mapped" in {

      val ebookSearch: ActorRef[SearchCommand] =
        MockEbookSearch(testKit, None, Some(mapperFailure))

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, ebookAnalyticsClient, Some(ebookSearch))

      lazy val routes: Route =
        new Routes(ebookRegistry, itemRegistry, pssRegistry, apiKeyRegistry).applicationRoutes

      val request = Get(s"/v2/ebooks/R0VfVX4BfY91SSpFGqxt?api_key=$fakeApiKey")
        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/json`))))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.InternalServerError
        contentType should === (ContentTypes.`application/json`)
      }
    }
  }
}
