package dpla.api.v2.endToEnd

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.helpers.ActorHelper
import dpla.api.helpers.Utils.fakeApiKey
import dpla.api.v2.registry.{ApiKeyRegistryCommand, MockApiKeyRegistry, MockItemRegistry, MockPssRegistry, MockSmrRegistry, SearchRegistryCommand, SmrRegistryCommand}
import dpla.api.v2.search.SearchProtocol.SearchCommand
import dpla.api.v2.search.mappings.JsonFieldReader
import dpla.api.v2.search.MockItemSearch
import dpla.api.v2.smr.MockSmrRequestHandler
import dpla.api.v2.smr.SmrProtocol.SmrCommand
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._

class HappyPathsTest extends AnyWordSpec with Matchers with ScalatestRouteTest
  with JsonFieldReader with ActorHelper {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val itemSearch: ActorRef[SearchCommand] =
    MockItemSearch(testKit, Some(itemElasticSearchClient), Some(dplaMapMapper))

  val smrRequestHandler: ActorRef[SmrCommand] =
    MockSmrRequestHandler(testKit, Some(s3ClientSuccess))

  val apiKeyRegistryWithEmailSuccess: ActorRef[ApiKeyRegistryCommand] =
    MockApiKeyRegistry(testKit, authenticator, Some(emailClient))

  val itemRegistryEsSuccess: ActorRef[SearchRegistryCommand] =
    MockItemRegistry(testKit, authenticator, itemAnalyticsClient, Some(itemSearch))

  val smrRegistryS3Success: ActorRef[SmrRegistryCommand] =
    MockSmrRegistry(testKit, authenticator, Some(smrRequestHandler))

  lazy val routes: Route =
    new Routes(itemRegistryEsSuccess, pssRegistry,
      apiKeyRegistryWithEmailSuccess, smrRegistryS3Success).applicationRoutes

  "/v2/ebooks route" should {
    "return NotFound after ebook removal" in {
      val request = Get("/v2/ebooks")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.NotFound
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

  "/v2/smr route" should {
    "be happy with valid input and successful s3 upload" in {
      val service = "tiktok"
      val post = "123"
      val user = "abc"

      val postData = JsObject(
        "service" -> service.toJson,
        "post" -> post.toJson,
        "user" -> user.toJson
      ).toString

      val request = HttpRequest(
        POST,
        uri = "/v2/smr",
        entity = HttpEntity(ContentTypes.`application/json`, postData),
        headers = List(RawHeader("Authorization", fakeApiKey))
      )

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }

  "/v2/random route" should {
    "be happy with empty params and successful es response" in {
      val request = Get(s"/v2/random?api_key=$fakeApiKey")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
        contentType should === (ContentTypes.`application/json`)
      }
    }
  }

  "/v2/api_key/[email] route" should {
    "be happy with valid input, successful db write, & successful email" in {
      val validEmail = "test@example.com"
      val request = Post(s"/v2/api_key/$validEmail")

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }
}
