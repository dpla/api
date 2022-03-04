package dpla.ebookapi.v1.apiKey

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.{EmailClient, PostgresClient}
import dpla.ebookapi.v1.PostgresClient.PostgresClientCommand
import dpla.ebookapi.v1.EmailClient.EmailClientCommand
import dpla.ebookapi.v1.apiKey.ApiKeyParamValidator.ApiKeyValidationCommand


/**
 * Handles the control flow for processing a request from Routes.
 */
object ApiKeyRegistry extends ApiKeyRegistryBehavior {

  override def spawnParamValidator(
                                    context: ActorContext[ApiKeyRegistryCommand]
                                  ): ActorRef[ApiKeyValidationCommand] =
    context.spawn(ApiKeyParamValidator(), "ApiKeyParamValidator")


  override def spawnAuthenticationClient(
                                    context: ActorContext[ApiKeyRegistryCommand]
                                  ): ActorRef[PostgresClientCommand] =
    context.spawn(PostgresClient(), "AuthenticationClientForApiKeys")

  override def spawnEmailClient(
                                 context: ActorContext[ApiKeyRegistryCommand]
                               ): ActorRef[EmailClientCommand] =
    context.spawn(EmailClient(), "EmailClientForApiKeys")
}
