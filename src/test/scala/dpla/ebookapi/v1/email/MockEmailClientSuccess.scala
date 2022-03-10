package dpla.ebookapi.v1.email

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.email.EmailClient.{EmailClientCommand, SendEmail}

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
