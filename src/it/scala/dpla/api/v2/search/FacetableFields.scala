package dpla.api.v2.search

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, LogCapturing}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
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


/**
 * Test that expected fields are facetable in item search.
 * Facet by coordinates is not included here as it requires special syntax.
 */
class FacetableFields extends AnyWordSpec with Matchers with ScalatestRouteTest
  with LogCapturing {

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

  val facetableFields = Seq(
    "admin.contributingInstitution",
    "dataProvider",
    "dataProvider.@id",
    "dataProvider.exactMatch",
    "dataProvider.name",
    "hasView.@id",
    "hasView.format",
    "intermediateProvider",
    "isPartOf.@id",
    "isPartOf.name",
    "provider.@id",
    "provider.exactMatch",
    "provider.name",
    "rights",
    "rightsCategory",
    "sourceResource.collection.title",
    "sourceResource.contributor",
    "sourceResource.date.begin",
    "sourceResource.date.begin.month",
    "sourceResource.date.begin.year",
    "sourceResource.date.end",
    "sourceResource.date.end.month",
    "sourceResource.date.end.year",
    "sourceResource.format",
    "sourceResource.language.iso639_3",
    "sourceResource.language.name",
    "sourceResource.publisher",
    "sourceResource.spatial.city",
    "sourceResource.spatial.country",
    "sourceResource.spatial.county",
    "sourceResource.spatial.name",
    "sourceResource.spatial.region",
    "sourceResource.spatial.state",
    "sourceResource.subject.@id",
    "sourceResource.subject.name",
    "sourceResource.temporal.begin",
    "sourceResource.temporal.end",
    "sourceResource.title",
    "sourceResource.type"
  )

  for (facet <- facetableFields) {

    s"Facet by $facet" should {
      "return OK" in {

        val request = Get(s"/v2/items?api_key=$fakeApiKey&page_size=0&facets=$facet")

        request ~> routes ~> check {
          status shouldEqual StatusCodes.OK
        }
      }
    }
  }
}
