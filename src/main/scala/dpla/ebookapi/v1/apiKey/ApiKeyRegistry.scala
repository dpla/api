package dpla.ebookapi.v1.apiKey

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.EmailClient
import dpla.ebookapi.v1.EmailClient.EmailClientCommand
import dpla.ebookapi.v1.authentication.AuthProtocol.AuthenticationCommand
import dpla.ebookapi.v1.authentication.Authenticator


/**
 * Handles the control flow for processing a request from Routes.
 */
object ApiKeyRegistry extends ApiKeyRegistryBehavior {

  override def spawnAuthenticator(
                                   context: ActorContext[ApiKeyRegistryCommand]
                                 ): ActorRef[AuthenticationCommand] =
    context.spawn(Authenticator(), "AuthenticatorForApiKeys")

  override def spawnEmailClient(
                                 context: ActorContext[ApiKeyRegistryCommand]
                               ): ActorRef[EmailClientCommand] =
    context.spawn(EmailClient(), "EmailClientForApiKeys")
}
