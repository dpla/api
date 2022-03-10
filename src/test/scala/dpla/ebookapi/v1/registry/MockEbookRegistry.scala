package dpla.ebookapi.v1.registry

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.analytics.AnalyticsClient.AnalyticsClientCommand
import dpla.ebookapi.v1.authentication.AuthProtocol.AuthenticationCommand
import dpla.ebookapi.v1.search.EbookSearch
import dpla.ebookapi.v1.search.SearchProtocol.SearchCommand

object MockEbookRegistry {

  def apply(
             testKit: ActorTestKit,
             authenticator: ActorRef[AuthenticationCommand],
             analyticsClient: ActorRef[AnalyticsClientCommand],
             ebookSearch: Option[ActorRef[SearchCommand]] = None
           ): ActorRef[EbookRegistryCommand] = {

    object Mock extends EbookRegistryBehavior {

      override def spawnEbookSearch(
                                     context: ActorContext[EbookRegistryCommand]
                                   ): ActorRef[SearchCommand] =
        ebookSearch.getOrElse(context.spawnAnonymous(EbookSearch()))
    }

    testKit.spawn(Mock(authenticator, analyticsClient))
  }
}
