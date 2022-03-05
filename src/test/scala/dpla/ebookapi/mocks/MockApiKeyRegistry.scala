package dpla.ebookapi.mocks

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.EmailClient.EmailClientCommand
import dpla.ebookapi.v1.EmailClient
import dpla.ebookapi.v1.apiKey.{ApiKeyRegistryBehavior, ApiKeyRegistryCommand}
import dpla.ebookapi.v1.authentication.{Authenticator, AuthenticatorCommand}


class MockApiKeyRegistry(testKit: ActorTestKit) {

  private var authenticator: Option[ActorRef[AuthenticatorCommand]] = None
  private var emailClient: Option[ActorRef[EmailClientCommand]] = None

  def setAuthenticator(ref: ActorRef[AuthenticatorCommand]): Unit =
    authenticator = Some(ref)

  def setEmailClient(ref: ActorRef[EmailClientCommand]): Unit =
    emailClient = Some(ref)

  object Mock extends ApiKeyRegistryBehavior {

    override def spawnAuthenticator(
                                     context: ActorContext[ApiKeyRegistryCommand]
                                   ): ActorRef[AuthenticatorCommand] =
      authenticator.getOrElse(context.spawnAnonymous(Authenticator()))

    override def spawnEmailClient(
                                   context: ActorContext[ApiKeyRegistryCommand]
                                 ): ActorRef[EmailClientCommand] =
      emailClient.getOrElse(context.spawnAnonymous(EmailClient()))
  }

  def getRef: ActorRef[ApiKeyRegistryCommand] = testKit.spawn(Mock())
}
