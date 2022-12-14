package dpla.api.v2.search

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, LogCapturing}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Route
import dpla.api.Routes
import dpla.api.helpers.{FileReader, ITHelper}
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
class PssTests extends ITHelper with LogCapturing with FileReader {

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

    "return correct fields for the source" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val sourceFields = Seq(
          "@id",
          "@type",
          "disambiguatingDescription",
          "name",
          "mainEntity",
          "repImageUrl",
          "thumbnailUrl"
        )

        val source: JsObject = readObjectArray(entity, "hasPart")
          .find(part => readString(part, "disambiguatingDescription").contains("source"))
          .get

        val mainEntity: JsObject = readObjectArray(source, "mainEntity").head

        source.fields.keys should contain allElementsOf sourceFields
        mainEntity.fields.keys should contain("@type")
      }
    }

    "include the guide" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val traversed: Option[JsValue] = readObjectArray(entity, "hasPart")
          .find(part => readString(part, "disambiguatingDescription").contains("guide"))

        traversed should not be empty
      }
    }

    "include the overview" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val traversed: Option[JsValue] = readObjectArray(entity, "hasPart")
          .find(part => readString(part, "name").contains("Overview"))

        traversed should not be empty
      }
    }

    "include the resources" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val traversed: Option[JsValue] = readObjectArray(entity, "hasPart")
          .find(part => readString(part, "name").contains("Resources"))

        traversed should not be empty
      }
    }
  }

  "each set endpoint" should {

    "match the given slug" in {
      readFile("/set_slugs.txt").foreach(slug => {
        implicit val request: HttpRequest =
          Get(s"/v2/pss/sets/$slug?api_key=$fakeApiKey")

        request ~> routes ~> check {
          val entity: JsObject = entityAs[String].parseJson.asJsObject
          val traversed = readString(entity, "@id").get
          assert(traversed.endsWith(s"/sets/$slug"))
        }
      })
    }
  }

  "single source endpoint" should {
    implicit val request: HttpRequest =
      Get(s"/v2/pss/sources/1441?api_key=$fakeApiKey")

    returnStatusCode(200)
    returnJSON

    "include correct fields" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val fields = Seq(
          "@context",
          "@id",
          "@type",
          "dct:created",
          "dct:modified",
          "dateCreated",
          "dateModified",
          "isPartOf",
          "isRelatedTo",
          "mainEntity",
          "name",
          "repImageUrl",
          "text",
          "thumbnailUrl"
        )

        entity.fields.keys should contain allElementsOf fields
      }
    }
  }

  "each source endpoint" should {

    "match the given id" in {
      val someSourceIds = Seq(
        "1918",
        "1441",
        "1234",
        "378",
        "99",
        "34",
        "5",
        "1"
      )

      someSourceIds.foreach(id => {
        implicit val request: HttpRequest =
          Get(s"/v2/pss/sources/$id?api_key=$fakeApiKey")

        request ~> routes ~> check {
          val entity: JsObject = entityAs[String].parseJson.asJsObject
          val traversed = readString(entity, "@id").get
          assert(traversed.endsWith(s"/sources/$id"))
        }
      })
    }
  }
}