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

  val smrRegistry: ActorRef[SmrRegistryCommand] =
    testKit.spawn(SmrRegistry(authenticator))

  val routes: Route =
    new Routes(ebookRegistry, itemRegistry, pssRegistry, apiKeyRegistry,
      smrRegistry).applicationRoutes

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
          "dateCreated",
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

    "includes the guide author" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val guide: Option[JsValue] = readObjectArray(entity, "hasPart")
          .find(part => readString(part, "disambiguatingDescription").contains("guide"))

        val traversed = guide.map(g => readObjectArray(g.asJsObject, "author"))

        traversed should not be empty
      }
    }

    "includes the guide parts" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val guide: Option[JsValue] = readObjectArray(entity, "hasPart")
          .find(part => readString(part, "disambiguatingDescription").contains("guide"))

        val traversed = guide.map(g => readObjectArray(g.asJsObject, "hasPart"))

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

  "sort by recently added" should {

    "sort correctly" in {
      implicit val request: HttpRequest =
        Get(s"/v2/pss/sets?api_key=$fakeApiKey&order=recently_added")

      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val dates = readObjectArray(entity, "itemListElement")
          .map(set => readString(set, "dateCreated").get)
        val sorted = dates.sorted.reverse
        dates.zip(sorted).foreach{ case(d1, d2) => assert(d1==d2) }
      }
    }
  }

  "filter by about.name" should {

    val timePeriod = "Postwar%20United%20States%20(1945%20to%20early%201970s)"
    val subject = "Labor%20History"

    implicit val request: HttpRequest =
      Get(s"/v2/pss/sets?api_key=$fakeApiKey&filter=about.name:$timePeriod+AND+$subject")

    returnStatusCode(200)
    returnJSON
    returnInt("numberOfItems", 2)

    "include given filter terms" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val aboutNames = readObjectArray(entity, "itemListElement")
          .flatMap(set =>
            readObjectArray(set, "about")
            .map(about => readString(about, "name").get)
          )
        aboutNames should contain (timePeriod.replaceAll("%20", " "))
        aboutNames should contain (subject.replaceAll("%20", " "))
      }
    }
  }
}
