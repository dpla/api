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

  "Temporal search, sourceResource.temporal.after" should {
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
              // null is allowed, move on
              if (end != null) {
                // parse the date
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

  "Temporal search, sourceResource.date.after" should {
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

          // iterate through the dates
          readObjectArray(doc, "sourceResource.date").map(temporal => {
            readString(temporal, "end").map(end => {
              // null is allowed, move on
              if (end != null) {
                // parse the date
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
          val fields = doc.fields.keys
          expectedFields should contain allElementsOf fields
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

  "Multiple facets" should {
    val facet1 = "dataProvider"
    val facet2 = "sourceResource.publisher"

    val request = Get(s"/v2/items?api_key=$fakeApiKey&fields=id,dataProvider,sourceResource.title&facets=$facet1,$facet2")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return the given facet fields" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val facetKeys = readObject(entity, "facets").map(_.fields.keys)
          .getOrElse(Iterable())
        facetKeys should contain allOf (facet1, facet2)
      }
    }
  }

  "Temporal Search, sourceResource.date within a range" should {
    val queryAfter = "1960"
    val queryBefore = "1980"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&sourceResource.date.after=$queryAfter&sourceResource.date.before=$queryBefore&fields=sourceResource.date&page_size=500")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return docs with dates that overlap the given range" in {
      val afterDate = new SimpleDateFormat("yyyy").parse(queryAfter)
      val beforeDate = new SimpleDateFormat("yyyy").parse(queryBefore)

      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        // iterate through the docs
        readObjectArray(entity, "docs").foreach(doc => {
          // iterate through the dates
          readObject(doc, "sourceResource.date").foreach(date => {
            var beginOk = false
            var endOk = false

            readString(date, "begin") match {
              case Some(begin) =>
                if (begin == null) {
                  // null is allowed
                  beginOk = true
                } else {
                  // parse the date
                  dateFormats.flatMap(format => {
                    Try { new SimpleDateFormat(format).parse(begin) }.toOption
                  })
                    // get the first successfully parsed date (there should be only one)
                    .headOption.foreach(beginDate => {
                    // beginDate should be before or equal to beforeDate
                    if (beginDate.before(beforeDate) || beginDate.equals(beforeDate)) {
                      beginOk = true
                    }
                  })
                }
              case None =>
                // None is allowed
                beginOk = true
            }

            readString(date, "end") match {
              case Some(end) =>
                if (end == null) {
                  // null is allowed
                  endOk = true
                } else {
                  // parse the date
                  dateFormats.flatMap(format => {
                    Try {
                      new SimpleDateFormat(format).parse(end)
                    }.toOption
                  })
                    // get the first successfully parsed date (there should be only one)
                    .headOption.foreach(endDate => {
                    // endDate should be after or equal to afterDate
                    if (endDate.after(afterDate) || endDate.equals(afterDate)) {
                      endOk = true
                    }
                  })
                }
              case None =>
                // None is allowed
                endOk = true
            }

            beginOk shouldBe true
            endOk shouldBe true
          })
        })
      }
    }
  }

  "Page size, within bounds" should {
    val requestSize = "2"
    val expectedSize = 2
    val request = Get(s"/v2/items?api_key=$fakeApiKey&page_size=$requestSize")

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

  "Simple search" should {
    val query = "test"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&q=$query")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "match a word in each doc" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        readObjectArray(entity, "docs").map(doc => {
          doc.toString.toLowerCase should include(query)
        })
      }
    }
  }

  "Temporal search, sourceResource.date.before" should {
    val queryBefore = "1980"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&sourceResource.date.before=$queryBefore&fields=sourceResource.date&page_size=500")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return docs with date ranges that come before the given date" in {
      val queryDate = new SimpleDateFormat("yyyy").parse(queryBefore)

      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        // iterate through the docs
        readObjectArray(entity, "docs").map(doc => {
          // will be made true if at least 1 of the dates in this array fits
          var isOk = false

          readObjectArray(doc, "sourceResource.date").map(date => {
            readString(date, "begin").map(begin => {
              // null is allowed, move on
              if (begin != null) {
                // parse the date
                dateFormats.flatMap(format => {
                  Try { new SimpleDateFormat(format).parse(begin) }.toOption
                })
                  // get the first successfully parsed date (there should be only one)
                  .headOption.map(beginDate => {
                  // beginDate should be greater than or equal to query date
                  if (beginDate.before(queryDate) || beginDate.equals(queryDate)) {
                    isOk = true
                  }
                })
              }
            })
          })

          isOk shouldBe true
        })
      }
    }
  }

  "Bad request: search on originalRecord" should {
    val queryTerm = "no+good"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&originalRecord.stringValue=$queryTerm")

    "return status code 400" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }

  "Multiple items, comma-separated" should {
    val id1 = "00002e1fe8817b91ef4a9ef65a212a18"
    val id2 = "cc7a1cbdeec0681cdb14ad0f315de3a9"
    val request = Get(s"/v2/items/$id1,$id2?api_key=$fakeApiKey")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return a count of two" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val count: Option[Int] = readInt(entity, "count")
        count should === (Some(2))
      }
    }

    "return an array with two docs" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val docs = readObjectArray(entity, "docs")
        docs.size should === (2)
      }
    }
  }

  "Spatial Search, state" should {
    val state = "Hawaii"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&sourceResource.spatial.state=$state&fields=sourceResource.spatial&page_size=500")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return docs with state names that match the query" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        // iterate through docs
        readObjectArray(entity, "docs").map(doc => {

          // get all state names for this doc
          val stateNames = readObjectArray(doc, "sourceResource.spatial").flatMap(spatial => {
            readString(spatial, "state")
          })

          stateNames should contain(state)
        })
      }
    }
  }

  "Facet, no documents" should {
    val facet1 = "dataProvider"
    val facet2 = "sourceResource.publisher"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&fields=id,dataProvider,sourceResource.title&facets=$facet1,$facet2&page_size=0")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return 0 docs" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val docs = readObjectArray(entity, "docs")
        docs.size should === (0)
      }
    }

    "return the given facet fields" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val facetKeys = readObject(entity, "facets").map(_.fields.keys)
          .getOrElse(Iterable())
        facetKeys should contain allOf(facet1, facet2)
      }
    }
  }

  "Sort by field, spatial coordinates" should {
    val request = Get(s"/v2/items?api_key=$fakeApiKey&sort_by=sourceResource.spatial.coordinates&fields=sourceResource.spatial&sort_by_pin=42,-70")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }

  "Q and field filters combined" should {
    val query = "test"
    val titleQuery = "tube"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&q=$query&sourceResource.title=$titleQuery")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return docs with q keyword in sourceResource" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        readObjectArray(entity, "docs").map(doc => {
          val sourceResource = readObject(doc, "sourceResource").toString
            .toLowerCase

          sourceResource should include(query)
        })
      }
    }

    "return docs with title keyword in title" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        readObjectArray(entity, "docs").map(doc => {
          val titles = readStringArray(doc, "sourceResource", "title")
            .mkString(" ").toLowerCase

           titles should include(titleQuery)
        })
      }
    }
  }

  "Facet, basic" should {
    val facet = "dataProvider"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&fields=id,dataProvider,sourceResource.title&facets=$facet")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return the given facet field" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val facets = readObject(entity, "facets").map(_.fields.keys)
          .getOrElse(Iterable())
        facets should contain only facet
      }
    }

    "have multiple terms for the facet" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val terms = readObjectArray(entity, "facets", facet)
        terms.length shouldBe > (0)
      }
    }

    "have a term and count for the facet" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val facetTerms = readObjectArray(entity, "facets", facet, "terms")
          .headOption.map(_.fields.keys).getOrElse(Iterable())
        facetTerms should contain allOf ("term", "count")
      }
    }
  }

  "Exact field match" should {
    val queryTerm = "University+of+Pennsylvania"
    val expectedTerm = "University of Pennsylvania"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&dataProvider=$queryTerm&exact_field_match=true&page_size=500&fields=dataProvider")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return docs with the exact term in the given field" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        readObjectArray(entity, "docs").map(doc => {
          val dataProvider = readString(doc, "dataProvider", "name")

          dataProvider should === (Some(expectedTerm))
        })
      }
    }
  }

  "Match-all query" should {
    val request = Get(s"/v2/items?api_key=$fakeApiKey")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return 10 docs" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val docs = readObjectArray(entity, "docs")
        docs.size shouldBe 10
      }
    }

    "have a count of greater than 10" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val count = readInt(entity, "count").get
        count should be > 10
      }
    }

    "have a limit of 10" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val limit = readInt(entity, "limit")
        limit shouldBe Some(10)
      }
    }

    "have an empty facets array" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val facets = readObjectArray(entity, "facets")
        facets.size shouldBe 0
      }
    }
  }

  "Temporal search, sourceResource.temporal.before" should {
    val queryBefore = "1980"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&sourceResource.temporal.before=$queryBefore&fields=sourceResource.temporal&page_size=500")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return docs with date ranges that come before the given date" in {
      val queryDate = new SimpleDateFormat("yyyy").parse(queryBefore)

      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        // iterate through the docs
        readObjectArray(entity, "docs").map(doc => {
          // will be made true if at least 1 of the dates in this array fits
          var isOk = false

          readObjectArray(doc, "sourceResource.temporal").map(date => {
            readString(date, "begin").map(begin => {
              // null is allowed, move on
              if (begin != null) {
                // parse the date
                dateFormats.flatMap(format => {
                  Try {
                    new SimpleDateFormat(format).parse(begin)
                  }.toOption
                })
                  // get the first successfully parsed date (there should be only one)
                  .headOption.map(beginDate => {
                  // beginDate should be greater than or equal to query date
                  if (beginDate.before(queryDate) || beginDate.equals(queryDate)) {
                    isOk = true
                  }
                })
              }
            })
          })

          isOk shouldBe true
        })
      }
    }
  }

  "Facet, date with year modifier" should {
    val facetName = "sourceResource.date.begin.year"
    val after = "1000"
    val before = "2020"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&facets=$facetName&page_size=0&sourceResource.date.after=$after&sourceResource.date.before=$before")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "have facet values at least one year apart" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        // iterate through the docs
        val times: Seq[Int] = readObjectArray(entity, "facets", facetName, "entries")
          .flatMap(entry => {
            val dateString = readString(entry, "time").getOrElse("")
            // parse the date
            dateFormats.flatMap(format => {
              Try { new SimpleDateFormat(format).parse(dateString) }.toOption
            }).headOption
              // transform date into year
              .map(date => {
                val yearFormatter = new SimpleDateFormat("yyyy")
                yearFormatter.format(date).toInt
              })
          })

        val sorted = times.sorted.reverse // sort largest to smallest
        val left = sorted.dropRight(1) // left side of equation
        val right = sorted.drop(1) // right side of equation
        val zipped = left.zip(right)
        val differences = zipped.map({ case (a,b) => a-b }) // subtract
        val min = differences.min // smallest difference between two subsequent dates

        min should be >= 1
      }
    }
  }

  "Search by field" should {
    val field = "sourceResource.description"
    val query = "president"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&$field=$query&fields=$field&page_size=500")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return docs with the given term in the correct field" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        readObjectArray(entity, "docs").map(doc => {
          val fieldValue = readStringArray(doc, field).mkString(" ").toLowerCase
          fieldValue should include(query)
        })
      }
    }
  }

  "Facet, spatial distance ranges" should {
    val fieldName = "sourceResource.spatial.coordinates"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&facets=$fieldName:42.3:-71&page_size=0")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "have facet with correctly-named property" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val properties = readObject(entity, "facets").map(_.fields.keys)
          .getOrElse(Seq())
        properties should contain only fieldName
      }
    }

    "return facet with _type and ranges properties" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val properties = readObject(entity, "facets", fieldName)
          .map(_.fields.keys).getOrElse(Seq())
        properties should contain allOf ("_type", "ranges")
      }
    }

    "return a facet with _type geo_distance" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val typeProperty = readString(entity, "facets", fieldName, "_type")
        typeProperty shouldBe Some("geo_distance")
      }
    }

    "return a facet with a non-empty array of ranges" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val ranges = readObjectArray(entity, "facets", fieldName, "ranges")
        ranges.size should be > 0
      }
    }

    "return ranges with the correct properties" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        // The first element of 'ranges' lacks the 'from' property,
        // and the last one lacks the 'to' property,
        // but the 2nd element (index 1) should have both.
        val properties = readObjectArray(entity, "facets", fieldName, "ranges")(2)
          .fields.keys
        properties should contain allOf ("from", "to", "count")
      }
    }
  }

  "Boolean OR" should {
    val term1 = "president"
    val term2 = "congress"
    val request = Get(s"/v2/items?api_key=$fakeApiKey&q=$term1+OR+$term2")

    "return status code 200" in {
      request ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return docs with one of the search terms in sourceResource" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        readObjectArray(entity, "docs").map({ doc =>
          val sourceResource = readObject(doc, "sourceResource")
            .toString.toLowerCase
          sourceResource should (include (term1) or include (term2))
        })
      }
    }
  }
}
