package dpla.api.v2.authentication
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.api.v2.authentication.AuthProtocol.{AuthenticationCommand, IntermediateAuthResult}

/**
 * Handles user accounts and api keys.
 */
object Authenticator extends AuthenticatorBehavior {

  override def spawnPostgresClient(
                           context: ActorContext[AuthenticationCommand]
                         ): ActorRef[IntermediateAuthResult] =
    context.spawn(PostgresClient(), "PostgresClient")
}
