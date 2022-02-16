package dpla.ebookapi.mocks

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.PostgresClient.{CreateApiKey, FindApiKey, PostgresClientCommand}
import dpla.ebookapi.v1.PostgresError

object MockPostgresClientError {

  def apply(): Behavior[PostgresClientCommand] = {
    Behaviors.receiveMessage[PostgresClientCommand] {

      case FindApiKey(_, replyTo) =>
        replyTo ! PostgresError
        Behaviors.same

      case CreateApiKey(_, replyTo) =>
        replyTo ! PostgresError
        Behaviors.same
    }
  }
}
