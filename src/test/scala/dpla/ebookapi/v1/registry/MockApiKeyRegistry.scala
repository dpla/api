package dpla.ebookapi.v1.registry

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.email.EmailClient.EmailClientCommand
import dpla.ebookapi.v1.authentication.AuthProtocol.AuthenticationCommand
import dpla.ebookapi.v1.authentication.Authenticator
import dpla.ebookapi.v1.email.EmailClient


class MockApiKeyRegistry(testKit: ActorTestKit) {

  private var authenticator: Option[ActorRef[AuthenticationCommand]] = None
  private var emailClient: Option[ActorRef[EmailClientCommand]] = None

  def setAuthenticator(ref: ActorRef[AuthenticationCommand]): Unit =
    authenticator = Some(ref)

  def setEmailClient(ref: ActorRef[EmailClientCommand]): Unit =
    emailClient = Some(ref)

  object Mock extends ApiKeyRegistryBehavior {

    override def spawnAuthenticator(
                                     context: ActorContext[ApiKeyRegistryCommand]
                                   ): ActorRef[AuthenticationCommand] =
      authenticator.getOrElse(context.spawnAnonymous(Authenticator()))

    override def spawnEmailClient(
                                   context: ActorContext[ApiKeyRegistryCommand]
                                 ): ActorRef[EmailClientCommand] =
      emailClient.getOrElse(context.spawnAnonymous(EmailClient()))
  }

  def getRef: ActorRef[ApiKeyRegistryCommand] = testKit.spawn(Mock())
}
