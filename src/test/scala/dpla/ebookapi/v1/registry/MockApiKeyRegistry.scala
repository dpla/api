package dpla.ebookapi.v1.registry

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.email.EmailClient.EmailClientCommand
import dpla.ebookapi.v1.authentication.AuthProtocol.AuthenticationCommand
import dpla.ebookapi.v1.email.EmailClient

class MockApiKeyRegistry(
                          testKit: ActorTestKit,
                          authenticator: ActorRef[AuthenticationCommand]
                        ) {

  private var emailClient: Option[ActorRef[EmailClientCommand]] = None

  def setEmailClient(ref: ActorRef[EmailClientCommand]): Unit =
    emailClient = Some(ref)

  object Mock extends ApiKeyRegistryBehavior {

    override def spawnEmailClient(
                                   context: ActorContext[ApiKeyRegistryCommand]
                                 ): ActorRef[EmailClientCommand] =
      emailClient.getOrElse(context.spawnAnonymous(EmailClient()))
  }

  def getRef: ActorRef[ApiKeyRegistryCommand] =
    testKit.spawn(Mock(authenticator))
}
