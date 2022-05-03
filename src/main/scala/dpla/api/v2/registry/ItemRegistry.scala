package dpla.api.v2.registry

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.api.v2.search.ItemSearch
import dpla.api.v2.search.SearchProtocol.SearchCommand

/**
 * Handles the control flow for processing an ebooks request from Routes.
 */
object ItemRegistry extends SearchRegistryBehavior {

  override def spawnSearchActor(
                                 context: ActorContext[SearchRegistryCommand]
                               ): ActorRef[SearchCommand] =
    context.spawn(ItemSearch(), "ItemSearch")

  override val searchType: String = "Item"
}
