package dpla.api.v2.registry

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.api.v2.analytics.AnalyticsClientCommand
import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
import dpla.api.v2.search.PssSearch
import dpla.api.v2.search.SearchProtocol.SearchCommand

object MockPssRegistry {

  def apply(
             testKit: ActorTestKit,
             authenticator: ActorRef[AuthenticationCommand],
             analyticsClient: ActorRef[AnalyticsClientCommand],
             pssSearch: Option[ActorRef[SearchCommand]] = None
           ): ActorRef[SearchRegistryCommand] = {

    object Mock extends SearchRegistryBehavior {

      override def spawnSearchActor(
                                     context: ActorContext[SearchRegistryCommand]
                                   ): ActorRef[SearchCommand] =
        pssSearch.getOrElse(context.spawnAnonymous(PssSearch()))
    }

    testKit.spawn(Mock(authenticator, analyticsClient))
  }
}
