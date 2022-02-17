package dpla.ebookapi.mocks

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.PostgresClient._
import dpla.ebookapi.v1.{UserAccount, AccountCreated, AccountFound}

object MockPostgresClientDisabled {

  private val apiKey = UserAccount(
    apiKey = "08e3918eeb8bf4469924f062072459a8",
    email = "x@example.org",
    staff = false,
    enabled = false
  )

  def apply(): Behavior[PostgresClientCommand] = {
    Behaviors.receiveMessage[PostgresClientCommand] {

      case FindAccount(_, replyTo) =>
        replyTo ! AccountFound(apiKey)
        Behaviors.same

      case CreateAccount(_, replyTo) =>
        replyTo ! AccountCreated(apiKey)
        Behaviors.same
    }
  }
}
