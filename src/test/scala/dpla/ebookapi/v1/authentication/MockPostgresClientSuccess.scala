package dpla.ebookapi.v1.authentication

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.authentication
import dpla.ebookapi.v1.authentication.AuthProtocol.{AccountCreated, AccountFound, IntermediateAuthResult, ValidApiKey, ValidEmail}

object MockPostgresClientSuccess {

  private val account = authentication.Account(
    id = 1,
    key = "08e3918eeb8bf4469924f062072459a8",
    email = "x@example.org",
    staff = Some(false),
    enabled = Some(true)
  )

  def apply(): Behavior[IntermediateAuthResult] = {
    Behaviors.receiveMessage[IntermediateAuthResult] {

      case ValidApiKey(_, replyTo) =>
        replyTo ! AccountFound(account)
        Behaviors.same

      case ValidEmail(_, replyTo) =>
        replyTo ! AccountCreated(account)
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}