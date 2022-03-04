package dpla.ebookapi.v1.apiKey

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.{EmailClient, ParamValidator, PostgresClient}
import dpla.ebookapi.v1.PostgresClient.PostgresClientCommand
import ParamValidator.ValidationCommand
import dpla.ebookapi.v1.EmailClient.EmailClientCommand


/**
 * Handles the control flow for processing a request from Routes.
 */
object ApiKeyRegistry extends ApiKeyRegistryBehavior {

  override def spawnParamValidator(
                                    context: ActorContext[ApiKeyRegistryCommand]
                                  ): ActorRef[ValidationCommand] =
    context.spawn(ParamValidator(), "ParamValidatorForApiKeys")


  override def spawnAuthenticationClient(
                                    context: ActorContext[ApiKeyRegistryCommand]
                                  ): ActorRef[PostgresClientCommand] =
    context.spawn(PostgresClient(), "AuthenticationClientForApiKeys")

  override def spawnEmailClient(
                                 context: ActorContext[ApiKeyRegistryCommand]
                               ): ActorRef[EmailClientCommand] =
    context.spawn(EmailClient(), "EmailClientForApiKeys")
}
