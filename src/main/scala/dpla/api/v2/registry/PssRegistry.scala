package dpla.api.v2.registry

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.api.v2.search.PssSearch
import dpla.api.v2.search.SearchProtocol.SearchCommand

/**
 * Handles the control flow for processing a primary source sets request from
 * Routes.
 */
object PssRegistry extends SearchRegistryBehavior {

  override def spawnSearchActor(
                                 context: ActorContext[SearchRegistryCommand]
                               ): ActorRef[SearchCommand] =
    context.spawn(PssSearch(), "PssSearch")
}
