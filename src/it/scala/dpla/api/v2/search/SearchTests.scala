package dpla.api.v2.search

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
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

  "Filter by dataProvider ESDN" should {
    "return the correct doc count" in {
      val request = Get(s"/v2/items?api_key=$fakeApiKey&filter=provider.%40id:http%3A%2F%2Fdp.la%2Fapi%2Fcontributor%2Fesdn")

      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val count: Option[Int] = readInt(entity, "count")
        count should === (Some(443200))
      }
    }
  }

  "Search by phrase" should {
    "match exactly a phrase in each doc's sourceResource" in {
      val searchPhrase = "\"old\\+victorian\""
      val request = Get(s"/v2/items?api_key=$fakeApiKey&q=$searchPhrase")

      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        readObjectArray(entity, "docs").map(doc => {
          val sourceResource = readObject(doc, "sourceResource").toString
            .toLowerCase

          sourceResource should include("old victorian")
        })
      }
    }
  }

  "Temporal search by sourceResource.temporal.after" should {
    "return docs with date ranges that come after the given date" in {
      val request = Get(s"/v2/items?api_key=$fakeApiKey&sourceResource.temporal.after=1960&fields=sourceResource.temporal&page_size=500")

      val dateFormats = Seq(
        "yyyy",
        "yyyy-MM",
        "yyyy-MM-dd"
      )
      val queryTime = new SimpleDateFormat("yyyy").parse("1960")

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
                  if (endDate.after(queryTime) || endDate.equals(queryTime)) {
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
}
