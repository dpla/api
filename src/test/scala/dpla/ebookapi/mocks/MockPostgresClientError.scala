package dpla.ebookapi.mocks

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.PostgresClient._
import dpla.ebookapi.v1.PostgresError

object MockPostgresClientError {

  def apply(): Behavior[PostgresClientCommand] = {
    Behaviors.receiveMessage[PostgresClientCommand] {

      case FindAccountByKey(_, replyTo) =>
        replyTo ! PostgresError
        Behaviors.same

      case FindAccountByEmail(_, replyTo) =>
        replyTo ! PostgresError
        Behaviors.same

      case CreateAccount(_, replyTo) =>
        replyTo ! PostgresError
        Behaviors.same
    }
  }
}
