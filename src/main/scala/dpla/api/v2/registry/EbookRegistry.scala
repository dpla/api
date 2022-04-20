package dpla.api.v2.registry

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.api.v2.search.EbookSearch
import dpla.api.v2.search.SearchProtocol.SearchCommand

/**
 * Handles the control flow for processing an ebooks request from Routes.
 */
object EbookRegistry extends EbookRegistryBehavior {

  override def spawnEbookSearch(
                                 context: ActorContext[EbookRegistryCommand]
                               ): ActorRef[SearchCommand] =
    context.spawn(EbookSearch(), "EbookSearch")
}
