package dpla.ebookapi.mocks

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.PostgresClient._
import dpla.ebookapi.v1.{AccountNotFound, PostgresError}

object MockPostgresClientUnsuccessful {

  def apply(): Behavior[PostgresClientCommand] = {
    Behaviors.receiveMessage[PostgresClientCommand] {

      case FindAccountByKey(_, replyTo) =>
        replyTo ! AccountNotFound
        Behaviors.same

      case FindAccountByEmail(_, replyTo) =>
        replyTo ! AccountNotFound
        Behaviors.same

      case CreateAccount(_, replyTo) =>
        replyTo ! PostgresError
        Behaviors.same
    }
  }
}
