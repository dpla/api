package dpla.api.v2.authentication

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.api.v2.authentication.AuthProtocol.{AuthenticationCommand, IntermediateAuthResult}

object ITMockAuthenticator {

  def apply(
             testKit: ActorTestKit,
             postgresClient: Option[ActorRef[IntermediateAuthResult]] = None
           ): ActorRef[AuthenticationCommand] = {

    object Mock extends AuthenticatorBehavior {

      override def spawnPostgresClient(
                                        context: ActorContext[AuthenticationCommand]
                                      ): ActorRef[IntermediateAuthResult] =
        postgresClient.getOrElse(context.spawnAnonymous(PostgresClient()))
    }

    testKit.spawn(Mock())
  }
}
