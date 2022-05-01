package dpla.api.v2.registry

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.api.helpers.FileReader
import dpla.api.helpers.Utils.fakeApiKey
import dpla.api.v2.analytics.AnalyticsClient.{AnalyticsClientCommand, TrackFetch, TrackSearch}
import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
import dpla.api.v2.authentication.{MockAuthenticator, MockPostgresClientStaff, MockPostgresClientSuccess}
import dpla.api.v2.registry.RegistryProtocol.RegistryResponse
import dpla.api.v2.search.SearchProtocol.SearchCommand
import dpla.api.v2.search.{DPLAMAPMapper, MockEbookSearch, MockEsClientSuccess}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EbookRegistryTest extends AnyWordSpec with Matchers with FileReader
  with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  val mapper = testKit.spawn(DPLAMAPMapper())
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

      ebookRegistry ! SearchEbooks(Some(fakeApiKey),
        Map(), "", "", replyProbe.ref)

      analyticsProbe.expectMessageType[TrackSearch]
    }

    "not send analytics message if account is staff" in {
      val postgresClient = testKit.spawn(MockPostgresClientStaff())

      val authenticator: ActorRef[AuthenticationCommand] =
        MockAuthenticator(testKit, Some(postgresClient))

      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, analyticsProbe.ref, Some(ebookSearch))

      ebookRegistry ! SearchEbooks(Some(fakeApiKey),
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

      ebookRegistry ! FetchEbook(Some(fakeApiKey),
        "ufwPJ34Bj-MaVWqX9KZL", Map(), "", "", replyProbe.ref)

      analyticsProbe.expectMessageType[TrackFetch]
    }

    "not send analytics message if account is staff" in {
      val postgresClient = testKit.spawn(MockPostgresClientStaff())

      val authenticator: ActorRef[AuthenticationCommand] =
        MockAuthenticator(testKit, Some(postgresClient))

      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, analyticsProbe.ref, Some(ebookSearch))

      ebookRegistry ! FetchEbook(Some(fakeApiKey),
        "ufwPJ34Bj-MaVWqX9KZL", Map(), "", "", replyProbe.ref)

      analyticsProbe.expectNoMessage
    }
  }

  "EbookRegistry Multi-Fetch" should {

    "send analytics message if account is non-staff" in {
      val postgresClient = testKit.spawn(MockPostgresClientSuccess())

      val authenticator: ActorRef[AuthenticationCommand] =
        MockAuthenticator(testKit, Some(postgresClient))

      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, analyticsProbe.ref, Some(ebookSearch))

      ebookRegistry ! FetchEbook(Some(fakeApiKey),
        "b70107e4fe29fe4a247ae46e118ce192,17b0da7b05805d78daf8753a6641b3f5",
        Map(), "", "", replyProbe.ref)

      analyticsProbe.expectMessageType[TrackSearch]
    }

    "not send analytics message if account is staff" in {
      val postgresClient = testKit.spawn(MockPostgresClientStaff())

      val authenticator: ActorRef[AuthenticationCommand] =
        MockAuthenticator(testKit, Some(postgresClient))

      val ebookRegistry: ActorRef[EbookRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, analyticsProbe.ref, Some(ebookSearch))

      ebookRegistry ! FetchEbook(Some(fakeApiKey),
        "b70107e4fe29fe4a247ae46e118ce192,17b0da7b05805d78daf8753a6641b3f5",
        Map(), "", "", replyProbe.ref)

      analyticsProbe.expectNoMessage
    }
  }
}
