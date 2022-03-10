package dpla.ebookapi.v1.authentication

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.authentication.AuthProtocol.{AuthenticationCommand, IntermediateAuthResult}

class MockAuthenticator(testKit: ActorTestKit) {

  private var postgresClient: Option[ActorRef[IntermediateAuthResult]] = None

  def setPostgresClient(ref: ActorRef[IntermediateAuthResult]): Unit =
    postgresClient = Some(ref)

  object Mock extends AuthenticatorBehavior {

    override def spawnPostgresClient(
                                      context: ActorContext[AuthenticationCommand]
                                    ): ActorRef[IntermediateAuthResult] =
      postgresClient.getOrElse(context.spawnAnonymous(PostgresClient()))
  }

  def getRef: ActorRef[AuthenticationCommand] = testKit.spawn(Mock())
}
