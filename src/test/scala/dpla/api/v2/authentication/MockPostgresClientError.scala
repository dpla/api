package dpla.api.v2.authentication

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.api.v2.authentication.AuthProtocol.{AuthenticationFailure, IntermediateAuthResult, ValidApiKey, ValidEmail}

object MockPostgresClientError {

  def apply(): Behavior[IntermediateAuthResult] = {
    Behaviors.receiveMessage[IntermediateAuthResult] {

      case ValidApiKey(_, replyTo) =>
        replyTo ! AuthenticationFailure
        Behaviors.same

      case ValidEmail(_, replyTo) =>
        replyTo ! AuthenticationFailure
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}
