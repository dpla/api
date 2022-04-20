package dpla.api.v2.authentication

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.api.v2.authentication.AuthProtocol.{AccountNotFound, IntermediateAuthResult, ValidApiKey}

object MockPostgresClientKeyNotFound {

  def apply(): Behavior[IntermediateAuthResult] = {
    Behaviors.receiveMessage[IntermediateAuthResult] {

      case ValidApiKey(_, replyTo) =>
        replyTo ! AccountNotFound
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}
