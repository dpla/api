package dpla.ebookapi.v1.httpHeadersAndMethods

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.Routes
import dpla.ebookapi.v1.authentication.AuthProtocol.AuthenticationCommand
import dpla.ebookapi.v1.authentication.{MockAuthenticator, MockPostgresClientSuccess}
import dpla.ebookapi.v1.registry.{ApiKeyRegistryCommand, EbookRegistryCommand, MockApiKeyRegistry, MockEbookRegistry}
import dpla.ebookapi.v1.search.{EbookMapper, MockEbookSearch, MockEsClientSuccess}
import dpla.ebookapi.v1.search.SearchProtocol.SearchCommand
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PermittedMediaTypesTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem
  val postgresClient = testKit.spawn(MockPostgresClientSuccess())
  val mapper = testKit.spawn(EbookMapper())
  val elasticSearchClient = testKit.spawn(MockEsClientSuccess(mapper))

  val mockEbookSearch = new MockEbookSearch(testKit)
  mockEbookSearch.setElasticSearchClient(elasticSearchClient)
  mockEbookSearch.setEbookMapper(mapper)
  val ebookSearch: ActorRef[SearchCommand] =
    mockEbookSearch.getRef

  val mockAuthenticator = new MockAuthenticator(testKit)
  mockAuthenticator.setPostgresClient(postgresClient)
  val authenticator: ActorRef[AuthenticationCommand] = mockAuthenticator.getRef

  val mockEbookRegistry = new MockEbookRegistry(testKit, authenticator)
  mockEbookRegistry.setEbookSearch(ebookSearch)
  val ebookRegistry: ActorRef[EbookRegistryCommand] =
    mockEbookRegistry.getRef

  val mockApiKeyRegistry = new MockApiKeyRegistry(testKit, authenticator)
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
