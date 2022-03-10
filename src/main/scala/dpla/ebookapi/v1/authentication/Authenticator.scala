package dpla.ebookapi.v1.authentication
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.authentication.AuthProtocol.{AuthenticationCommand, IntermediateAuthResult}

object Authenticator extends AuthenticatorBehavior {

  override def spawnPostgresClient(
                           context: ActorContext[AuthenticationCommand]
                         ): ActorRef[IntermediateAuthResult] =
    context.spawn(PostgresClient(), "PostgresClient")
}
