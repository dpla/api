package dpla.api.v2.registry

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.api.v2.analytics.AnalyticsClientCommand
import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
import dpla.api.v2.search.EbookSearch
import dpla.api.v2.search.SearchProtocol.SearchCommand

object MockEbookRegistry {

  def apply(
             testKit: ActorTestKit,
             authenticator: ActorRef[AuthenticationCommand],
             analyticsClient: ActorRef[AnalyticsClientCommand],
             ebookSearch: Option[ActorRef[SearchCommand]] = None
           ): ActorRef[SearchRegistryCommand] = {

    object Mock extends SearchRegistryBehavior {

      override def spawnSearchActor(
                                     context: ActorContext[SearchRegistryCommand]
                                   ): ActorRef[SearchCommand] =
        ebookSearch.getOrElse(context.spawnAnonymous(EbookSearch()))
    }

    testKit.spawn(Mock(authenticator, analyticsClient))
  }
}
