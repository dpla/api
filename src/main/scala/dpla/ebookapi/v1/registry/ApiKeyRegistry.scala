package dpla.ebookapi.v1.registry

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.email.EmailClient.EmailClientCommand
import dpla.ebookapi.v1.email.EmailClient

/**
 * Handles the control flow for processing a request from Routes.
 */
object ApiKeyRegistry extends ApiKeyRegistryBehavior {

  override def spawnEmailClient(
                                 context: ActorContext[ApiKeyRegistryCommand]
                               ): ActorRef[EmailClientCommand] =
    context.spawn(EmailClient(), "EmailClientForApiKeys")
}
