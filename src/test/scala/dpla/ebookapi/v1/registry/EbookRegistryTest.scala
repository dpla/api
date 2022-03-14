package dpla.ebookapi.v1.registry

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.ebookapi.helpers.FileReader
import dpla.ebookapi.v1.analytics.AnalyticsClient.{AnalyticsClientCommand, TrackFetch, TrackSearch}
import dpla.ebookapi.v1.authentication.AuthProtocol.AuthenticationCommand
import dpla.ebookapi.v1.authentication.{MockAuthenticator, MockPostgresClientStaff, MockPostgresClientSuccess}
import dpla.ebookapi.v1.registry.RegistryProtocol.RegistryResponse
import dpla.ebookapi.v1.search.SearchProtocol.SearchCommand
import dpla.ebookapi.v1.search.{EbookMapper, MockEbookSearch, MockEsClientSuccess}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EbookRegistryTest extends AnyWordSpec with Matchers with FileReader
  with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  val mapper = testKit.spawn(EbookMapper())
  val elasticSearchClient = testKit.spawn(MockEsClientSuccess(mapper))

  val analyticsProbe: TestProbe[AnalyticsClientCommand] =
    testKit.createTestProbe[AnalyticsClientCommand]

  val ebookSearch: ActorRef[SearchCommand] =
    MockEbookSearch(testKit, Some(elasticSearchClient), Some(mapper))

  val replyProbe: TestProbe[RegistryResponse] =
    testKit.createTestProbe[RegistryResponse]

  "EbookRegistry Search" should {

    "send analytics message if account is non-staff" in {
      val postgresClient = testKit.spawn(MockPostgresClientSuccess())

      val authenticator: ActorRef[AuthenticationCommand] =
        MockAuthenticator(testKit, Some(postgresClient))

      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, analyticsProbe.ref, Some(ebookSearch))

      ebookRegistry ! SearchEbooks(Some("08e3918eeb8bf4469924f062072459a8"),
        Map(), "", "", replyProbe.ref)

      analyticsProbe.expectMessageType[TrackSearch]
    }

    "not send analytics message if account is staff" in {
      val postgresClient = testKit.spawn(MockPostgresClientStaff())

      val authenticator: ActorRef[AuthenticationCommand] =
        MockAuthenticator(testKit, Some(postgresClient))

      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, analyticsProbe.ref, Some(ebookSearch))

      ebookRegistry ! SearchEbooks(Some("08e3918eeb8bf4469924f062072459a8"),
        Map(), "", "", replyProbe.ref)

      analyticsProbe.expectNoMessage
    }
  }

  "EbookRegistry Fetch" should {

    "send analytics message if account is non-staff" in {
      val postgresClient = testKit.spawn(MockPostgresClientSuccess())

      val authenticator: ActorRef[AuthenticationCommand] =
        MockAuthenticator(testKit, Some(postgresClient))

      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, analyticsProbe.ref, Some(ebookSearch))

      ebookRegistry ! FetchEbook(Some("08e3918eeb8bf4469924f062072459a8"),
        "ufwPJ34Bj-MaVWqX9KZL", Map(), "", "", replyProbe.ref)

      analyticsProbe.expectMessageType[TrackFetch]
    }

    "not send analytics message if account is staff" in {
      val postgresClient = testKit.spawn(MockPostgresClientStaff())

      val authenticator: ActorRef[AuthenticationCommand] =
        MockAuthenticator(testKit, Some(postgresClient))

      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, analyticsProbe.ref, Some(ebookSearch))

      ebookRegistry ! FetchEbook(Some("08e3918eeb8bf4469924f062072459a8"),
        "ufwPJ34Bj-MaVWqX9KZL", Map(), "", "", replyProbe.ref)

      analyticsProbe.expectNoMessage
    }
  }
}
