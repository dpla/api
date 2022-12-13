package dpla.api.v2.search

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, LogCapturing}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Route
import dpla.api.Routes
import dpla.api.helpers.ITHelper
import dpla.api.helpers.ITUtils.fakeApiKey
import dpla.api.v2.analytics.{AnalyticsClientCommand, ITMockAnalyticsClient}
import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
import dpla.api.v2.authentication.{ITMockAuthenticator, ITMockPostgresClient}
import dpla.api.v2.registry._
import spray.json._



/**
 * Test that expected fields are sortable in item search.
 * Sort by coordinates is not included here as it requires special syntax.
 */
class PssTests extends ITHelper with LogCapturing {

  lazy val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  // Stub out analytics client
  val analyticsClient: ActorRef[AnalyticsClientCommand] =
    testKit.spawn(ITMockAnalyticsClient())

  // Stub out authentication
  val postgresClient = testKit.spawn(ITMockPostgresClient())

  val authenticator: ActorRef[AuthenticationCommand] =
    ITMockAuthenticator(testKit, Some(postgresClient))

  val ebookRegistry: ActorRef[SearchRegistryCommand] =
    testKit.spawn(EbookRegistry(authenticator, analyticsClient))

  val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
    testKit.spawn(ApiKeyRegistry(authenticator))

  val itemRegistry: ActorRef[SearchRegistryCommand] =
    testKit.spawn(ItemRegistry(authenticator, analyticsClient))

  val pssRegistry: ActorRef[SearchRegistryCommand] =
    testKit.spawn(PssRegistry(authenticator, analyticsClient))

  val routes: Route =
    new Routes(ebookRegistry, itemRegistry, pssRegistry, apiKeyRegistry).applicationRoutes

  "all sets endpoint" should {
    implicit val request: HttpRequest =
      Get(s"/v2/pss/sets?api_key=$fakeApiKey")

    returnStatusCode(200)
    returnJSON
    returnString("@type", "ItemList")
    returnString("@context.@vocab", "http://schema.org/")
    returnInt("numberOfItems", 142)
    returnArrayWithSize("itemListElement", 142)

    "return correct fields for the set list" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val fields = Seq(
          "@context",
          "@type",
          "hasPart",
          "itemListElement",
          "numberOfItems"
//          TODO "url"?
        )

        entity.fields.keys should contain allElementsOf fields
      }
    }

    "return correct fields for each set" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val firstSet = readObjectArray(entity, "itemListElement").head

        val setFields = Seq(
          "@id",
          "@type",
          "about",
          "name",
//          TODO "numberOfItems"?
          "repImageUrl",
          "thumbnailUrl"
        )

        firstSet.fields.keys should contain allElementsOf setFields
      }
    }

    // TODO hasPart?
  }

  "single set endpoint" should {
    implicit val request: HttpRequest =
      Get(s"/v2/pss/sets/aviation?api_key=$fakeApiKey")

    returnStatusCode(200)
    returnJSON

    "return correct fields for the set" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val fields = Seq(
          "@context",
          "@id",
          "@type",
          "dct:created",
          "dct:modified",
          "dct:type",
          "accessibilityControl",
          "accessibilityFeature",
          "accessibilityHazard",
          "about",
          "author",
          "dateCreated",
          "dateModified",
          "description",
          "educationalAlignment",
          "hasPart",
          "inLanguage",
          "isRelatedTo",
          "learningResourceType",
          "license",
          "name",
          "publisher",
          "repImageUrl",
          "thumbnailUrl",
          "typicalAgeRange"
        )

        entity.fields.keys should contain allElementsOf fields
      }
    }
  }
}