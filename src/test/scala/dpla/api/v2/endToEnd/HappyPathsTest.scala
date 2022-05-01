package dpla.api.v2.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.helpers.Utils.fakeApiKey
import dpla.api.v2.analytics.AnalyticsClient
import dpla.api.v2.analytics.AnalyticsClient.AnalyticsClientCommand
import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
import dpla.api.v2.authentication.{MockAuthenticator, MockPostgresClientSuccess}
import dpla.api.v2.email.{EmailClient, MockEmailClientSuccess}
import dpla.api.v2.registry.{ApiKeyRegistryCommand, EbookRegistryCommand, MockApiKeyRegistry, MockEbookRegistry}
import dpla.api.v2.search.SearchProtocol.SearchCommand
import dpla.api.v2.search.{DPLAMAPMapper, JsonFieldReader, MockEbookSearch, MockEsClientSuccess}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._

class HappyPathsTest extends AnyWordSpec with Matchers with ScalatestRouteTest
  with JsonFieldReader {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val analyticsClient: ActorRef[AnalyticsClientCommand] =
    testKit.spawn(AnalyticsClient())
  val postgresClient = testKit.spawn(MockPostgresClientSuccess())
  val emailClient: ActorRef[EmailClient.EmailClientCommand] =
    testKit.spawn(MockEmailClientSuccess())
  val mapper = testKit.spawn(DPLAMAPMapper())
  val elasticSearchClient = testKit.spawn(MockEsClientSuccess(mapper))

  val authenticator: ActorRef[AuthenticationCommand] =
    MockAuthenticator(testKit, Some(postgresClient))

  val ebookSearch: ActorRef[SearchCommand] =
    MockEbookSearch(testKit, Some(elasticSearchClient), Some(mapper))

  val ebookRegistry: ActorRef[EbookRegistryCommand] =
    MockEbookRegistry(testKit, authenticator, analyticsClient, Some(ebookSearch))

  val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
    MockApiKeyRegistry(testKit, authenticator, Some(emailClient))

  lazy val routes: Route =
    new Routes(ebookRegistry, apiKeyRegistry).applicationRoutes

  "/v2/ebooks route" should {
    "be happy with valid user inputs and successful es response" in {
      val request = Get(s"/v2/ebooks?page_size=100&api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
        contentType should === (ContentTypes.`application/json`)

        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val limit: Option[Int] = readInt(entity, "limit")
        limit should === (Some(100))

        val expected = Seq(
          "ufwPJ34BjqMaVWqX9KZL",
          "uvwPJ34BjqMaVWqX9KZL",
          "uqwPJ34BjqMaVWqX9KZZ",
          "vPwPJ34BjqMaVWqX9KZZ",
          "vfwPJ34BjqMaVWqX9KZZ",
          "vvwPJ34BjqMaVWqX9KZZ",
          "vqwPJ34BjqMaVWqX9Kac",
          "wPwPJ34BjqMaVWqX9Kac",
          "wfwPJ34BjqMaVWqX9Kac",
          "wvwPJ34BjqMaVWqX9Kac"
        )
        val ids = readObjectArray(entity, "docs")
          .flatMap(readString(_, "id"))
        ids should contain allElementsOf expected
      }
    }
  }

  "/v2/ebooks/[id] route" should {
    "be happy with valid single ID and successful es response" in {
      val request = Get(s"/v2/ebooks/wfwPJ34Bj-MaVWqX9Kac?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
        contentType should === (ContentTypes.`application/json`)

        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val id = readObjectArray(entity, "docs")
          .flatMap(readString(_, "id")).headOption
        id should === (Some("wfwPJ34Bj-MaVWqX9Kac"))
      }
    }

    "be happy with valid multiple IDs and successful es response" in {
      val idSeq = Seq(
        "ufwPJ34BjqMaVWqX9KZL",
        "uvwPJ34BjqMaVWqX9KZL",
        "uqwPJ34BjqMaVWqX9KZZ",
        "vPwPJ34BjqMaVWqX9KZZ",
        "vfwPJ34BjqMaVWqX9KZZ",
        "vvwPJ34BjqMaVWqX9KZZ",
        "vqwPJ34BjqMaVWqX9Kac",
        "wPwPJ34BjqMaVWqX9Kac",
        "wfwPJ34BjqMaVWqX9Kac",
        "wvwPJ34BjqMaVWqX9Kac"
      )
      val ids = idSeq.mkString(",")
      val request = Get(s"/v2/ebooks/$ids?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
        contentType should === (ContentTypes.`application/json`)

        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val ids = readObjectArray(entity, "docs")
          .flatMap(readString(_, "id"))
        ids should contain allElementsOf idSeq
      }
    }
  }

  "/api_key/[email]" should {
    "be happy with valid input, successful db write, & successful email" in {
      val validEmail = "test@example.com"
      val request = Post(s"/v2/api_key/$validEmail")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }
}
