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
import dpla.api.v2.registry.{ApiKeyRegistryCommand, MockApiKeyRegistry, MockEbookRegistry, MockItemRegistry, MockPssRegistry, SearchRegistryCommand}
import dpla.api.v2.search.SearchProtocol.SearchCommand
import dpla.api.v2.search.{DPLAMAPMapper, JsonFieldReader, MockEbookSearch, MockEboookEsClientSuccess, MockItemEsClientSuccess, MockItemSearch}
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
  val ebookElasticSearchClient = testKit.spawn(MockEboookEsClientSuccess(mapper))
  val itemElasticSearchClient = testKit.spawn(MockItemEsClientSuccess(mapper))

  val authenticator: ActorRef[AuthenticationCommand] =
    MockAuthenticator(testKit, Some(postgresClient))

  val ebookSearch: ActorRef[SearchCommand] =
    MockEbookSearch(testKit, Some(ebookElasticSearchClient), Some(mapper))

  val itemSearch: ActorRef[SearchCommand] =
    MockItemSearch(testKit, Some(itemElasticSearchClient), Some(mapper))

  val ebookRegistry: ActorRef[SearchRegistryCommand] =
    MockEbookRegistry(testKit, authenticator, analyticsClient, Some(ebookSearch))

  val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
    MockApiKeyRegistry(testKit, authenticator, Some(emailClient))

  val itemRegistry: ActorRef[SearchRegistryCommand] =
    MockItemRegistry(testKit, authenticator, analyticsClient, Some(itemSearch))

  val pssRegistry: ActorRef[SearchRegistryCommand] =
    MockPssRegistry(testKit, authenticator, analyticsClient)

  lazy val routes: Route =
    new Routes(ebookRegistry, itemRegistry, pssRegistry, apiKeyRegistry)
      .applicationRoutes

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
          "3dbbf125aba2642e21f17c955bef4e96",
          "ccde9a5246356b1048873f9d9c71e5bb"
        )
        val ids = readObjectArray(entity, "docs")
          .flatMap(readString(_, "id"))
        ids should contain allElementsOf expected
      }
    }
  }

  "/v2/ebooks/[id] route" should {
    "be happy with valid single ID and successful es response" in {
      val request =
        Get(s"/v2/ebooks/3dbbf125aba2642e21f17c955bef4e96?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
        contentType should === (ContentTypes.`application/json`)

        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val id = readObjectArray(entity, "docs")
          .flatMap(readString(_, "id")).headOption
        id should === (Some("3dbbf125aba2642e21f17c955bef4e96"))
      }
    }

    "be happy with valid multiple IDs and successful es response" in {
      val idSeq = Seq(
        "3dbbf125aba2642e21f17c955bef4e96",
        "ccde9a5246356b1048873f9d9c71e5bb"
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

  "/v2/items route" should {
    "be happy with valid user inputs and successful es response" in {
      val request = Get(s"/v2/items?page_size=100&api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
        contentType should === (ContentTypes.`application/json`)

        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val limit: Option[Int] = readInt(entity, "limit")
        limit should === (Some(100))

        val expected = Seq(
          "4d85a6bd965dde8352c8235c43fe1c44",
          "c340ca5de0e46b0538213c65c650c8c6"
        )
        val ids = readObjectArray(entity, "docs")
          .flatMap(readString(_, "id"))
        ids should contain allElementsOf expected
      }
    }
  }

  "/v2/items/[id] route" should {
    "be happy with valid single ID and successful es response" in {
      val request =
        Get(s"/v2/items/4d85a6bd965dde8352c8235c43fe1c44?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
        contentType should === (ContentTypes.`application/json`)

        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val id = readObjectArray(entity, "docs")
          .flatMap(readString(_, "id")).headOption
        id should === (Some("4d85a6bd965dde8352c8235c43fe1c44"))
      }
    }

    "be happy with valid multiple IDs and successful es response" in {
      val idSeq = Seq(
        "4d85a6bd965dde8352c8235c43fe1c44",
        "c340ca5de0e46b0538213c65c650c8c6"
      )
      val ids = idSeq.mkString(",")
      val request = Get(s"/v2/items/$ids?api_key=$fakeApiKey")

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

  "v2/random route" should {
    "be happy with empty params and successful es response" in {
      val request = Get(s"/v2/random?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
        contentType should === (ContentTypes.`application/json`)
      }
    }
  }

  "/api_key/[email] route" should {
    "be happy with valid input, successful db write, & successful email" in {
      val validEmail = "test@example.com"
      val request = Post(s"/v2/api_key/$validEmail")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }
}
