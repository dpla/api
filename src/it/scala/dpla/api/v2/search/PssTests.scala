package dpla.api.v2.search

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, LogCapturing}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.Routes
import dpla.api.helpers.ITUtils.fakeApiKey
import dpla.api.v2.analytics.{AnalyticsClientCommand, ITMockAnalyticsClient}
import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
import dpla.api.v2.authentication.{ITMockAuthenticator, ITMockPostgresClient}
import dpla.api.v2.registry._
import dpla.api.v2.search.mappings.JsonFieldReader
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._

import scala.util.Try


/**
 * Test that expected fields are sortable in item search.
 * Sort by coordinates is not included here as it requires special syntax.
 */
class PssTests extends AnyWordSpec with Matchers with ScalatestRouteTest
  with JsonFieldReader with LogCapturing {

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

  /** Helper methods */

  private def returnStatusCode(code: Int)(implicit request: HttpRequest): Unit =
    s"return status code $code" in {
      request ~> routes ~> check {
        status.intValue shouldEqual code
      }
    }

  private def returnJSON(implicit request: HttpRequest): Unit =
    "return JSON" in {
      request ~> routes ~> check {
        val parsed = Try {
          entityAs[String].parseJson
        }.toOption
        parsed shouldNot be(None)
      }
    }

  private def includeField(field: String)(implicit request: HttpRequest): Unit =
    s"include field $field" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val path = field.split("\\.")
        val value: Option[JsValue] = readUnknown(entity, path:_*)
        value shouldNot be(None)
      }
    }

  /** Tests */

  "all sets endpoint" should {
    implicit val request = Get(s"/v2/pss/sets?api_key=$fakeApiKey")

    returnStatusCode(200)
    returnJSON

      val fields = Seq(
        "@context",
        "@type",
        "hasPart",
        "hasPart.@id",
        "itemListElement",
        "numberOfItems"
      )

    fields.foreach(includeField)
  }
}