package dpla.ebookapi.v1.registry

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.search.EbookSearch
import dpla.ebookapi.v1.search.SearchProtocol.SearchCommand

/**
 * Handles the control flow for processing an ebooks request from Routes.
 */

object EbookRegistry extends EbookRegistryBehavior {

  override def spawnEbookSearch(
                                 context: ActorContext[EbookRegistryCommand]
                               ): ActorRef[SearchCommand] =
    context.spawn(EbookSearch(), "EbookSearch")
}
