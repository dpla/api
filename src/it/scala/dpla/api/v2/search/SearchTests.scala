package dpla.api.v2.search

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.helpers.ITUtils.fakeApiKey
import dpla.api.v2.analytics.AnalyticsClient
import dpla.api.v2.analytics.AnalyticsClient.AnalyticsClientCommand
import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
import dpla.api.v2.authentication.{ITMockAuthenticator, ITMockPostgresClient}
import dpla.api.v2.registry.{ApiKeyRegistry, ApiKeyRegistryCommand, EbookRegistry, ItemRegistry, SearchRegistryCommand}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._

import java.text.SimpleDateFormat
import scala.util.Try


/**
 * Test that expected fields are sortable in item search.
 * Sort by coordinates is not included here as it requires special syntax.
 */
class SearchTests extends AnyWordSpec with Matchers with ScalatestRouteTest
  with JsonFieldReader {

  // All dates in the search index are expected to be in one of these formats.
  val dateFormats = Seq(
    "yyyy",
    "yyyy-MM",
    "yyyy-MM-dd"
  )

  lazy val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val analyticsClient: ActorRef[AnalyticsClientCommand] =
    testKit.spawn(AnalyticsClient())

  val postgresClient = testKit.spawn(ITMockPostgresClient())

  val authenticator: ActorRef[AuthenticationCommand] =
    ITMockAuthenticator(testKit, Some(postgresClient))

  val ebookRegistry: ActorRef[SearchRegistryCommand] =
    testKit.spawn(EbookRegistry(authenticator, analyticsClient))

  val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
    testKit.spawn(ApiKeyRegistry(authenticator))

  val itemRegistry: ActorRef[SearchRegistryCommand] =
    testKit.spawn(ItemRegistry(authenticator, analyticsClient))

  val routes: Route =
    new Routes(ebookRegistry, itemRegistry, apiKeyRegistry).applicationRoutes

  "Filter by provider" should {
    val providerId = "http://dp.la/api/contributor/esdn"
    val expectedCount = 443200
    val request = Get(s"/v2/items?api_key=$fakeApiKey&filter=provider.%40id:$providerId")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return the correct doc count" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val count: Option[Int] = readInt(entity, "count")
        count should === (Some(expectedCount))
      }
    }
  }

  "Search by phrase" should {
    val searchPhrase = "\"old\\+victorian\""
    val expectedPhrase = "old victorian"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&q=$searchPhrase")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "match exactly a phrase in each doc's sourceResource" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        readObjectArray(entity, "docs").map(doc => {
          val sourceResource = readObject(doc, "sourceResource").toString
            .toLowerCase

          sourceResource should include(expectedPhrase)
        })
      }
    }
  }

  "Temporal search by sourceResource.temporal.after" should {
    val queryAfter = "1960"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&sourceResource.temporal.after=$queryAfter&fields=sourceResource.temporal&page_size=500")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return docs with date ranges that come after the given date" in {
      val queryDate = new SimpleDateFormat("yyyy").parse(queryAfter)

      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        // iterate through the docs
        readObjectArray(entity, "docs").map(doc => {
          // will be made true if at least 1 of the dates in this array fits
          var isOk = false

          readObjectArray(doc, "sourceResource.temporal").map(temporal => {
            readString(temporal, "end").map(end => {
              // if sourceResource.temporal.end is null, that is allowed, move on
              if (end != null) {
                // parse the end date
                dateFormats.flatMap(format => {
                  Try { new SimpleDateFormat(format).parse(end) }.toOption
                })
                  // get the first successfully parsed date (there should be only one)
                  .headOption.map(endDate => {
                  // endDate should be greater than or equal to query date
                  if (endDate.after(queryDate) || endDate.equals(queryDate)) {
                    isOk = true
                  }
                })
              }
            })
          })

          // assert that the end date for this doc is equal to or after the query date
          isOk shouldBe true
        })
      }
    }
  }

  "Fetch one item by id" should {
    val itemId = "00002e1fe8817b91ef4a9ef65a212a18"
    val request = Get(s"/v2/items/$itemId?api_key=$fakeApiKey")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return a count of one" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val count: Option[Int] = readInt(entity, "count")
        count should === (Some(1))
      }
    }

    "return an array with a single doc" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val docs = readObjectArray(entity, "docs")
        docs.size should === (1)
      }
    }

    "return a doc with the correct id" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val docs = readObjectArray(entity, "docs")
        val id = docs.headOption.flatMap(doc => {
          readString(doc, "id")
        })
        id should === (Some(itemId))
      }
    }
  }

  "Spatial search by name" should {
    val searchTerm = "Boston"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&sourceResource.spatial=$searchTerm&fields=sourceResource.spatial&page_size=500")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "match term in at least one of each doc's place names" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        // iterate through the docs
        readObjectArray(entity, "docs").map(doc => {

          // get all values for sourceResource.spatial.name
          val spatialNames = readObjectArray(doc, "sourceResource.spatial").flatMap(spatial => {
            readString(spatial, "name")
          }).mkString(" ").toLowerCase

          spatialNames should include(searchTerm.toLowerCase)
        })
      }
    }
  }

  "Temporal search by sourceResource.date.after" should {
    val queryAfter = "1960"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&sourceResource.date.after=$queryAfter&fields=sourceResource.date&page_size=500")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return docs with date ranges that come after the given date" in {
      val queryDate = new SimpleDateFormat("yyyy").parse(queryAfter)

      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        // iterate through the docs
        readObjectArray(entity, "docs").map(doc => {
          // will be made true if at least 1 of the dates in this array fits
          var isOk = false

          readObjectArray(doc, "sourceResource.date").map(temporal => {
            readString(temporal, "end").map(end => {
              // if sourceResource.temporal.end is null, that is allowed, move on
              if (end != null) {
                // parse the end date
                dateFormats.flatMap(format => {
                  Try { new SimpleDateFormat(format).parse(end) }.toOption
                })
                  // get the first successfully parsed date (there should be only one)
                  .headOption.map(endDate => {
                  // endDate should be greater than or equal to query date
                  if (endDate.after(queryDate) || endDate.equals(queryDate)) {
                    isOk = true
                  }
                })
              }
            })
          })

          // assert that the end date for this doc is equal to or after the query date
          isOk shouldBe true
        })
      }
    }
  }

  "Wildcard pattern" should {
    val searchPhrase = "manuscr*"
    val expectedPhrase = "manuscr"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&q=$searchPhrase")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "match exactly a phrase in each doc's sourceResource" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        readObjectArray(entity, "docs").map(doc => {
          val sourceResource = readObject(doc, "sourceResource").toString
            .toLowerCase

          sourceResource should include(expectedPhrase)
        })
      }
    }
  }

  "Facet by field, coordinates" should {
    "return status code 200" in {
      val request = Get(s"/v2/items?api_key=$fakeApiKey&facets=sourceResource.spatial.coordinates:42:-70&page_size=0")

      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }

  "Page size, truncated" should {
    val requestSize = "501"
    val expectedSize = 500
    val request = Get(s"/v2/items?api_key=$fakeApiKey&page_size=$requestSize&fields=id")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    s"have limit of $expectedSize" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val limit: Option[Int] = readInt(entity, "limit")
        limit should === (Some(expectedSize))
      }
    }

    s"return $expectedSize docs" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val docs = readObjectArray(entity, "docs")
        docs.size should === (expectedSize)
      }
    }
  }

  "Boolean AND" should {
    val term1 = "fruit"
    val term2 = "banana"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&q=$term1+AND+$term2")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "match both terms in each doc" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        readObjectArray(entity, "docs").map(doc => {
          val docString = doc.toString.toLowerCase()
          docString should include(term1)
          docString should include(term2)
        })
      }
    }
  }

  "Date facet combo used by frontend" should {
    val request = Get(s"/v2/items?api_key=$fakeApiKey&page_size=0&facets=sourceResource.date.begin.year,sourceResource.date.end.year")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }

  "Sort by distance from a point" should {
    val coordinates = "38.897316,+-77.030027"
    val expectedPlaceName = "district of columbia"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&sort_by=sourceResource.spatial.coordinates&sort_by_pin=$coordinates&fields=sourceResource.spatial&page_size=5")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return top results reasonably close to pin" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        readObjectArray(entity, "docs").map(doc => {
          val docString = doc.toString.toLowerCase()
          docString should include(expectedPlaceName)
        })
      }
    }
  }

  "Only certain fields" should {
    val queryFields = "id,dataProvider,sourceResource.title"
    val expectedFields = Seq("id", "dataProvider", "sourceResource.title", "score")
    val request = Get(s"/v2/items?api_key=$fakeApiKey&fields=$queryFields")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return only the requested fields in each doc" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        readObjectArray(entity, "docs").map(doc => {
          doc.fields.keys.map(key => {
            expectedFields should contain(key)
          })
        })
      }
    }
  }

  "Facet with size limit" should {
    val queryFacetSize = "3"
    val expectedFacetSize = 3

    val request = Get(s"/v2/items?api_key=$fakeApiKey&facets=dataProvider&page_size=0&facet_size=$queryFacetSize")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "have facet size of the given limit" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val facets = readObjectArray(entity, "facets", "dataProvider", "terms")
        facets.size should === (expectedFacetSize)
      }
    }
  }
}
