package dpla.api.v2.registry

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.api.v2.email.EmailClient.EmailClientCommand
import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
import dpla.api.v2.email.EmailClient

object MockApiKeyRegistry {

  def apply(testKit: ActorTestKit,
            authenticator: ActorRef[AuthenticationCommand],
            emailClient: Option[ActorRef[EmailClientCommand]] = None
           ): ActorRef[ApiKeyRegistryCommand] = {

    object Mock extends ApiKeyRegistryBehavior {

      override def spawnEmailClient(
                                     context: ActorContext[ApiKeyRegistryCommand]
                                   ): ActorRef[EmailClientCommand] =
        emailClient.getOrElse(context.spawnAnonymous(EmailClient()))
    }

    testKit.spawn(Mock(authenticator))
  }
}
