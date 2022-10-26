package dpla.api.v2.search

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, LogCapturing}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.helpers.ITUtils.fakeApiKey
import dpla.api.v2.analytics.{AnalyticsClient, ITMockAnalyticsClient}
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
  with JsonFieldReader with LogCapturing {

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

  val routes: Route =
    new Routes(ebookRegistry, itemRegistry, apiKeyRegistry).applicationRoutes

  /** Helper methods */

  private def returnStatusCode(code: Int)(implicit request: HttpRequest): Unit =
    "return status code 200" in {
      request ~> routes ~> check {
        status.intValue shouldEqual code
      }
    }

  private def returnJSON(implicit request: HttpRequest): Unit =
    "return JSON" in {
      request ~> routes ~> check {
        val parsed = Try { entityAs[String].parseJson }.toOption
        parsed shouldNot be (None)
      }
    }

  private def returnCount(expected: Int)(implicit request: HttpRequest): Unit =
    s"return count $expected" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val count: Option[Int] = readInt(entity, "count")
        count should === (Some(expected))
      }
    }

  private def returnLimit(expected: Int)(implicit request: HttpRequest): Unit =
    s"return limit $expected" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val limit: Option[Int] = readInt(entity, "limit")
        limit should === (Some(expected))
      }
    }

  private def returnDocArrayWithSize(expected: Int)(implicit request: HttpRequest): Unit =
    s"return doc array with size $expected" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val docs = readObjectArray(entity, "docs")
        docs.size should === (expected)
      }
    }

  private def haveDateRangesAfter(queryAfter: String, field: String)(implicit request: HttpRequest): Unit =
    "return docs with date ranges that come after the given date" in {
      val queryDate = new SimpleDateFormat("yyyy").parse(queryAfter)

      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        // iterate through the docs
        readObjectArray(entity, "docs").map(doc => {
          // will be made true if at least 1 of the dates in this array fits
          var isOk = false

          readObjectArray(doc, field).map(temporal => {
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

//  private def returnDocWith(expected: String)(implicit request: HttpRequest): Unit =
//    s"return doc with '$expected''" in {
//
//    }

  /** Tests */

  "Filter by provider" should {
    val providerId = "http://dp.la/api/contributor/esdn"
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&filter=provider.%40id:$providerId")

    returnStatusCode(200)
    returnJSON
    returnCount(443200)
  }

  "Search for phrase" should {
    val searchPhrase = "\"old\\+victorian\""
    val expectedPhrase = "old victorian"
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&q=$searchPhrase")

    returnStatusCode(200)
    returnJSON

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
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&sourceResource.temporal.after=$queryAfter&fields=sourceResource.temporal&page_size=500")

    returnStatusCode(200)
    returnJSON
    haveDateRangesAfter("1960", "sourceResource.temporal")
  }

  "One item" should {
    val itemId = "00002e1fe8817b91ef4a9ef65a212a18"
    implicit val request = Get(s"/v2/items/$itemId?api_key=$fakeApiKey")

    returnStatusCode(200)
    returnJSON
    returnCount(1)
    returnDocArrayWithSize(1)

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
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&sourceResource.spatial=$searchTerm&fields=sourceResource.spatial&page_size=500")

    returnStatusCode(200)
    returnJSON

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
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&sourceResource.date.after=$queryAfter&fields=sourceResource.date&page_size=500")

    returnStatusCode(200)
    returnJSON
    haveDateRangesAfter("1960", "sourceResource.date")
  }

  "Wildcard pattern" should {
    val searchPhrase = "manuscr*"
    val expectedPhrase = "manuscr"
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&q=$searchPhrase")

    returnStatusCode(200)
    returnJSON

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
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&facets=sourceResource.spatial.coordinates:42:-70&page_size=0")

    returnStatusCode(200)
    returnJSON
  }

  "Page size, truncated" should {
    val requestSize = "501"
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&page_size=$requestSize&fields=id")

    returnStatusCode(200)
    returnJSON
    returnLimit(500)
    returnDocArrayWithSize(500)
  }

  "Boolean AND" should {
    val term1 = "fruit"
    val term2 = "banana"
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&q=$term1+AND+$term2")

    returnStatusCode(200)
    returnJSON

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

  "Facet, date facet combo used by frontend" should {
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&page_size=0&facets=sourceResource.date.begin.year,sourceResource.date.end.year")

    returnStatusCode(200)
    returnJSON
  }

  "Sort by distance from a point" should {
    val coordinates = "38.897316,+-77.030027"
    val expectedPlaceName = "district of columbia"
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&sort_by=sourceResource.spatial.coordinates&sort_by_pin=$coordinates&fields=sourceResource.spatial&page_size=5")

    returnStatusCode(200)
    returnJSON

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
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&fields=$queryFields")

    returnStatusCode(200)
    returnJSON

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
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&facets=dataProvider&page_size=0&facet_size=$queryFacetSize")

    returnStatusCode(200)
    returnJSON

    "have facet size of the given limit" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val facets = readObjectArray(entity, "facets", "dataProvider", "terms")
        facets.size should === (expectedFacetSize)
      }
    }
  }

  "Facet, multiple" should {
    val facet1 = "dataProvider"
    val facet2 = "sourceResource.publisher"
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&fields=id,dataProvider,sourceResource.title&facets=$facet1,$facet2")

    returnStatusCode(200)
    returnJSON

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
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&sourceResource.date.after=$queryAfter&sourceResource.date.before=$queryBefore&fields=sourceResource.date&page_size=500")

    returnStatusCode(200)
    returnJSON

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
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&page_size=$requestSize")

    returnStatusCode(200)
    returnJSON
    returnLimit(2)
    returnDocArrayWithSize(2)
  }

  "Simple search" should {
    val query = "test"
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&q=$query")

    returnStatusCode(200)
    returnJSON

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
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&sourceResource.date.before=$queryBefore&fields=sourceResource.date&page_size=500")

    returnStatusCode(200)
    returnJSON

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
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&originalRecord.stringValue=$queryTerm")

    returnStatusCode(400)
    returnJSON
  }

  "Multiple items, comma-separated" should {
    val id1 = "00002e1fe8817b91ef4a9ef65a212a18"
    val id2 = "cc7a1cbdeec0681cdb14ad0f315de3a9"
    implicit val request = Get(s"/v2/items/$id1,$id2?api_key=$fakeApiKey")

    returnStatusCode(200)
    returnJSON
    returnCount(2)
    returnDocArrayWithSize(2)
  }

  "Temporal Search, sourceResource.temporal within a range" should {
    val queryAfter = "1960"
    val queryBefore = "1980"
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&sourceResource.temporal.after=$queryAfter&sourceResource.temporal.before=$queryBefore&fields=sourceResource.temporal&page_size=500")

    returnStatusCode(200)
    returnJSON

    "return docs with dates that overlap the given range" in {
      val afterDate = new SimpleDateFormat("yyyy").parse(queryAfter)
      val beforeDate = new SimpleDateFormat("yyyy").parse(queryBefore)

      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        // iterate through the docs
        readObjectArray(entity, "docs").foreach(doc => {
          // iterate through the dates
          readObject(doc, "sourceResource.temporal").foreach(date => {
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

  "Spatial Search, state" should {
    val state = "Hawaii"
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&sourceResource.spatial.state=$state&fields=sourceResource.spatial&page_size=500")

    returnStatusCode(200)
    returnJSON

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
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&fields=id,dataProvider,sourceResource.title&facets=$facet1,$facet2&page_size=0")

    returnStatusCode(200)
    returnJSON
    returnDocArrayWithSize(0)

    "return the given facet fields" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val facetKeys = readObject(entity, "facets").map(_.fields.keys)
          .getOrElse(Iterable())
        facetKeys should contain allOf(facet1, facet2)
      }
    }
  }

  /**
   * SKIP
   * Though mentioned in https://pro.dp.la/developers/requests#spatial,
   * there is actually a test in query_spec.rb in the original platform app that
   * asserts that we ignore sourceResource.spatial.coordinates.
   *
   * "Spatial Search, coordinates, basic"
   * "/v2/items?api_key=$fakeApiKey&sourceResource.spatial.coordinates=20.75028,-156.50028&fields=sourceResource.spatial&page_size=500"
   * "return status code 200"
   * "return JSON"
   * "return a state Hawaii"
   * "return city Honolulu"
   */


  "Sort by field, spatial coordinates" should {
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&sort_by=sourceResource.spatial.coordinates&fields=sourceResource.spatial&sort_by_pin=42,-70")

    returnStatusCode(200)
    returnJSON
  }

  "Q and field filters combined" should {
    val query = "test"
    val titleQuery = "tube"
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&q=$query&sourceResource.title=$titleQuery")

    returnStatusCode(200)
    returnJSON

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
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&fields=id,dataProvider,sourceResource.title&facets=$facet")

    returnStatusCode(200)
    returnJSON

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
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&dataProvider=$queryTerm&exact_field_match=true&page_size=500&fields=dataProvider")

    returnStatusCode(200)
    returnJSON

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
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey")

    returnStatusCode(200)
    returnJSON
    returnLimit(10)
    returnDocArrayWithSize(10)

    "have a count of greater than 10" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val count = readInt(entity, "count").get
        count should be > 10
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
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&sourceResource.temporal.before=$queryBefore&fields=sourceResource.temporal&page_size=500")

    returnStatusCode(200)
    returnJSON

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
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&facets=$facetName&page_size=0&sourceResource.date.after=$after&sourceResource.date.before=$before")

    returnStatusCode(200)
    returnJSON

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
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&$field=$query&fields=$field&page_size=500")

    returnStatusCode(200)
    returnJSON

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
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&facets=$fieldName:42.3:-71&page_size=0")

    returnStatusCode(200)
    returnJSON

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
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&q=$term1+OR+$term2")

    returnStatusCode(200)
    returnJSON

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

  "Sort by title, ascending" should {
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&sort_by=sourceResource.title&fields=sourceResource.title")

    returnStatusCode(200)
    returnJSON

    /**
     * SKIP
     * Don't know how to test this because Scala sorts differently than
     * ElasticSearch on things like non-English characters.
     * Instead, I've added a test below that sorts by id.
     *
     * "sort titles in ascending order"
     */
  }

  "Sort by id, ascending" should {
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&sort_by=id&fields=id")

    returnStatusCode(200)
    returnJSON

    "sort ids in ascending order" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val ids = readObjectArray(entity, "docs").flatMap({ doc =>
          readString(doc, "id")
        })
        ids should contain theSameElementsInOrderAs ids.sorted
      }
    }
  }

  "404 for no docs, /v2/items/ID" should {
    // Assumes that this ID is not in the search index
    val id = "4b91c7105e6dff907d416d83ad1db450"
    implicit val request = Get(s"/v2/items/$id?api_key=$fakeApiKey")

    returnStatusCode(404)
    returnJSON
  }

  "Quoted isShownAt" should {
    implicit val request = Get(s"/v2/items?api_key=$fakeApiKey&&fields=id&isShownAt=%22http://digitalcollections.nypl.org/items/510d47dd-c6ef-a3d9-e040-e00a18064a99%22")

    returnStatusCode(200)
    returnJSON
  }
}
