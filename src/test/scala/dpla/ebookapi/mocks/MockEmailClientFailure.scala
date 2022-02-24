package dpla.ebookapi.mocks

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.EmailClient.{EmailClientCommand, SendEmail}
import dpla.ebookapi.v1.EmailFailure

object MockEmailClientFailure {

  def apply(): Behavior[EmailClientCommand] = {
    Behaviors.receiveMessage[EmailClientCommand] {

      case SendEmail(_, _, _, replyTo) =>
        replyTo ! EmailFailure
        Behaviors.same
    }
  }
}
