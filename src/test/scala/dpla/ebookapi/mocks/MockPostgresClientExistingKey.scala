package dpla.ebookapi.mocks

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.PostgresClient._
import dpla.ebookapi.v1.{UserAccount, AccountCreated, AccountFound}

object MockPostgresClientExistingKey {

  private val account = UserAccount(
    apiKey = "08e3918eeb8bf4469924f062072459a8",
    email = "x@example.org",
    staff = false,
    enabled = true
  )

  def apply(): Behavior[PostgresClientCommand] = {
    Behaviors.receiveMessage[PostgresClientCommand] {

      case FindAccountByKey(_, replyTo) =>
        Behaviors.unhandled

      case FindAccountByEmail(_, replyTo) =>
        Behaviors.unhandled

      case CreateAccount(_, replyTo) =>
        replyTo ! AccountFound(account)
        Behaviors.same
    }
  }
}
