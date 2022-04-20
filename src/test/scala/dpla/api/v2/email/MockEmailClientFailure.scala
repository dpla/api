package dpla.api.v2.email

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.api.v2.email.EmailClient.{EmailClientCommand, SendEmail}

object MockEmailClientFailure {

  def apply(): Behavior[EmailClientCommand] = {
    Behaviors.receiveMessage[EmailClientCommand] {

      case SendEmail(_, _, _, replyTo) =>
        replyTo ! EmailFailure
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}
