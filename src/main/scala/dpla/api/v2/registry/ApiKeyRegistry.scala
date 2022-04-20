package dpla.api.v2.registry

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.api.v2.email.EmailClient.EmailClientCommand
import dpla.api.v2.email.EmailClient

/**
 * Handles the control flow for processing an api_key request from Routes.
 */
object ApiKeyRegistry extends ApiKeyRegistryBehavior {

  override def spawnEmailClient(
                                 context: ActorContext[ApiKeyRegistryCommand]
                               ): ActorRef[EmailClientCommand] =
    context.spawn(EmailClient(), "EmailClientForApiKeys")
}
