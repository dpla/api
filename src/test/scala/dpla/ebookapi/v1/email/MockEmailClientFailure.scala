package dpla.ebookapi.v1.email

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.email.EmailClient.{EmailClientCommand, SendEmail}

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
