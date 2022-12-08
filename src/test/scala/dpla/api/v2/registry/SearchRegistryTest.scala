package dpla.api.v2.registry

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.api.helpers.{ActorHelper, FileReader}
import dpla.api.helpers.Utils.fakeApiKey
import dpla.api.v2.analytics.{AnalyticsClientCommand, TrackSearch}
import dpla.api.v2.registry.RegistryProtocol.RegistryResponse
import dpla.api.v2.search.SearchProtocol.SearchCommand
import dpla.api.v2.search.MockEbookSearch
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SearchRegistryTest extends AnyWordSpec with Matchers with FileReader
  with BeforeAndAfterAll with ActorHelper {

  lazy val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  val analyticsProbe: TestProbe[AnalyticsClientCommand] =
    testKit.createTestProbe[AnalyticsClientCommand]

  val ebookSearch: ActorRef[SearchCommand] =
    MockEbookSearch(testKit, Some(ebookElasticSearchClient), Some(dplaMapMapper))

  val replyProbe: TestProbe[RegistryResponse] =
    testKit.createTestProbe[RegistryResponse]

  "SearchRegistry Search" should {

    "send analytics message if account is non-staff" in {

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, analyticsProbe.ref, Some(ebookSearch))

      ebookRegistry ! RegisterSearch(Some(fakeApiKey),
        Map(), "", "", replyProbe.ref)

      analyticsProbe.expectMessageType[TrackSearch]
    }

    "not send analytics message if account is staff" in {

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticatorStaff, analyticsProbe.ref, Some(ebookSearch))

      ebookRegistry ! RegisterSearch(Some(fakeApiKey),
        Map(), "", "", replyProbe.ref)

      analyticsProbe.expectNoMessage
    }
  }

  "SearchRegistry Fetch" should {

    "send analytics message if account is non-staff" in {

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, analyticsProbe.ref, Some(ebookSearch))

      ebookRegistry ! RegisterFetch(Some(fakeApiKey),
        "ufwPJ34Bj-MaVWqX9KZL", Map(), "", "", replyProbe.ref)

      analyticsProbe.expectMessageType[TrackSearch]
    }

    "not send analytics message if account is staff" in {

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticatorStaff, analyticsProbe.ref, Some(ebookSearch))

      ebookRegistry ! RegisterFetch(Some(fakeApiKey),
        "ufwPJ34Bj-MaVWqX9KZL", Map(), "", "", replyProbe.ref)

      analyticsProbe.expectNoMessage
    }
  }

  "SearchRegistry Multi-Fetch" should {

    "send analytics message if account is non-staff" in {

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticator, analyticsProbe.ref, Some(ebookSearch))

      ebookRegistry ! RegisterFetch(Some(fakeApiKey),
        "b70107e4fe29fe4a247ae46e118ce192,17b0da7b05805d78daf8753a6641b3f5",
        Map(), "", "", replyProbe.ref)

      analyticsProbe.expectMessageType[TrackSearch]
    }

    "not send analytics message if account is staff" in {

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        MockEbookRegistry(testKit, authenticatorStaff, analyticsProbe.ref, Some(ebookSearch))

      ebookRegistry ! RegisterFetch(Some(fakeApiKey),
        "b70107e4fe29fe4a247ae46e118ce192,17b0da7b05805d78daf8753a6641b3f5",
        Map(), "", "", replyProbe.ref)

      analyticsProbe.expectNoMessage
    }
  }
}
