package dpla.ebookapi.v1.authentication

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.authentication.AuthProtocol.{AccountNotFound, IntermediateAuthResult, ValidApiKey}

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
