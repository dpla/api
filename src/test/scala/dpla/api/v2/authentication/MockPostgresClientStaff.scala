package dpla.api.v2.authentication

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.api.helpers.Utils.fakeApiKey
import dpla.api.v2.authentication
import dpla.api.v2.authentication.AuthProtocol.{AccountFound, IntermediateAuthResult, ValidApiKey}

object MockPostgresClientStaff {

  private val account = authentication.Account(
    id = 1,
    key = fakeApiKey,
    email = "x@dp.la",
    staff = Some(false),
    enabled = Some(true)
  )

  def apply(): Behavior[IntermediateAuthResult] = {
    Behaviors.receiveMessage[IntermediateAuthResult] {

      case ValidApiKey(_, replyTo) =>
        replyTo ! AccountFound(account)
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}
