package dpla.ebookapi.v1.authentication
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.ebookapi.v1.authentication.PostgresClient.PostgresClientCommand

object Authenticator extends AuthenticatorBehavior {

  override def spawnPostgresClient(
                           context: ActorContext[AuthenticatorCommand]
                         ): ActorRef[PostgresClientCommand] =
    context.spawn(PostgresClient(), "DatabaseClient")
}
