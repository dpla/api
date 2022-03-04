package dpla.ebookapi.mocks

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.EmailClient.EmailClientCommand
import dpla.ebookapi.v1.ParamValidator.ValidationCommand
import dpla.ebookapi.v1.PostgresClient.PostgresClientCommand
import dpla.ebookapi.v1.{EmailClient, ParamValidator, PostgresClient}
import dpla.ebookapi.v1.apiKey.{ApiKeyRegistryBehavior, ApiKeyRegistryCommand}

class MockApiKeyRegistry(testKit: ActorTestKit) {

  private var paramValidator: Option[ActorRef[ValidationCommand]] = None
  private var authenticationClient: Option[ActorRef[PostgresClientCommand]] = None
  private var emailClient: Option[ActorRef[EmailClientCommand]] = None

  def setParmaValidator(ref: ActorRef[ValidationCommand]): Unit =
    paramValidator = Some(ref)

  def setAuthenticationClient(ref: ActorRef[PostgresClientCommand]): Unit =
    authenticationClient = Some(ref)

  def setEmailClient(ref: ActorRef[EmailClientCommand]): Unit =
    emailClient = Some(ref)

  object Mock extends ApiKeyRegistryBehavior {
    override def spawnParamValidator(
                                      context: ActorContext[ApiKeyRegistryCommand]
                                    ): ActorRef[ValidationCommand] =
      paramValidator.getOrElse(context.spawnAnonymous(ParamValidator()))

    override def spawnAuthenticationClient(
                                      context: ActorContext[ApiKeyRegistryCommand]
                                    ): ActorRef[PostgresClientCommand] =
      authenticationClient.getOrElse(context.spawnAnonymous(PostgresClient()))

    override def spawnEmailClient(
                                   context: ActorContext[ApiKeyRegistryCommand]
                                 ): ActorRef[EmailClientCommand] =
      emailClient.getOrElse(context.spawnAnonymous(EmailClient()))
  }

  def getRef: ActorRef[ApiKeyRegistryCommand] = testKit.spawn(Mock())
}
