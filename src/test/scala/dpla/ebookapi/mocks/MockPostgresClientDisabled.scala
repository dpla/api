package dpla.ebookapi.mocks

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.authentication.PostgresClient._
import dpla.ebookapi.v1.authentication
import dpla.ebookapi.v1.authentication.{UserCreated, UserFound}

object MockPostgresClientDisabled {

  private val account = authentication.Account(
    id = 1,
    key = "08e3918eeb8bf4469924f062072459a8",
    email = "x@example.org",
    staff = Some(false),
    enabled = Some(false)
  )

  def apply(): Behavior[PostgresClientCommand] = {
    Behaviors.receiveMessage[PostgresClientCommand] {

      case FindUserByKey(_, replyTo) =>
        replyTo ! UserFound(account)
        Behaviors.same

      case CreateUser(_, replyTo) =>
        replyTo ! UserCreated(account)
        Behaviors.same
    }
  }
}
