package dpla.ebookapi.v1.registry

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.analytics.AnalyticsClient.AnalyticsClientCommand
import dpla.ebookapi.v1.analytics.AnalyticsClient
import dpla.ebookapi.v1.authentication.AuthProtocol.AuthenticationCommand
import dpla.ebookapi.v1.search.EbookSearch
import dpla.ebookapi.v1.search.SearchProtocol.SearchCommand

class MockEbookRegistry(
                         testKit: ActorTestKit,
                         authenticator: ActorRef[AuthenticationCommand],
                         analyticsClient: ActorRef[AnalyticsClientCommand]
                       ) {

  private var ebookSearch: Option[ActorRef[SearchCommand]] = None

  def setEbookSearch(ref: ActorRef[SearchCommand]): Unit =
    ebookSearch = Some(ref)

  object Mock extends EbookRegistryBehavior {

    override def spawnEbookSearch(
                                   context: ActorContext[EbookRegistryCommand]
                                 ): ActorRef[SearchCommand] =
      ebookSearch.getOrElse(context.spawnAnonymous(EbookSearch()))
  }

  def getRef: ActorRef[EbookRegistryCommand] =
    testKit.spawn(Mock(authenticator, analyticsClient))
}
