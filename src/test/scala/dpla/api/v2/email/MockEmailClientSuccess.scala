package dpla.api.v2.email

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.api.v2.email.EmailClient.{EmailClientCommand, SendEmail}

object MockEmailClientSuccess {

  def apply(): Behavior[EmailClientCommand] = {
    Behaviors.receiveMessage[EmailClientCommand] {

      case SendEmail(_, _, _, replyTo) =>
        replyTo ! EmailSuccess
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}
