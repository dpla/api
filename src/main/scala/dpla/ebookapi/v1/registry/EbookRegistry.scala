package dpla.ebookapi.v1.registry

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.AnalyticsClient
import dpla.ebookapi.v1.AnalyticsClient.AnalyticsClientCommand
import dpla.ebookapi.v1.authentication.AuthProtocol.AuthenticationCommand
import dpla.ebookapi.v1.authentication.Authenticator
import dpla.ebookapi.v1.search.EbookSearch
import dpla.ebookapi.v1.search.SearchProtocol.SearchCommand

/**
 * Handles the control flow for processing an ebooks request from Routes.
 */

object EbookRegistry extends EbookRegistryBehavior {

  override def spawnAuthenticator(
                                   context: ActorContext[EbookRegistryCommand]
                                 ): ActorRef[AuthenticationCommand] =
    context.spawn(Authenticator(), "EbookAuthenticator")

  override def spawnEbookSearch(
                                 context: ActorContext[EbookRegistryCommand]
                               ): ActorRef[SearchCommand] =
    context.spawn(EbookSearch(), "EbookSearch")

  override def spawnAnalyticsClient(
                                     context: ActorContext[EbookRegistryCommand]
                                   ): ActorRef[AnalyticsClientCommand] =
    context.spawn(AnalyticsClient(), "AnalyticsClientForEbooks")
}
