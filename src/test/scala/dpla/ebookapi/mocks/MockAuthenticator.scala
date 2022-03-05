package dpla.ebookapi.mocks

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.authentication.{AuthenticatorBehavior, AuthenticatorCommand, PostgresClient}
import dpla.ebookapi.v1.authentication.PostgresClient.PostgresClientCommand

class MockAuthenticator(testKit: ActorTestKit) {

  private var postgresClient: Option[ActorRef[PostgresClientCommand]] = None

  def setPostgresClient(ref: ActorRef[PostgresClientCommand]): Unit =
    postgresClient = Some(ref)

  object Mock extends AuthenticatorBehavior {

    override def spawnPostgresClient(
                                      context: ActorContext[AuthenticatorCommand]
                                    ): ActorRef[PostgresClientCommand] =
      postgresClient.getOrElse(context.spawnAnonymous(PostgresClient()))
  }

  def getRef: ActorRef[AuthenticatorCommand] = testKit.spawn(Mock())
}
