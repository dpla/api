package dpla.ebookapi.mocks

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.EmailClient.{EmailClientCommand, SendEmail}
import dpla.ebookapi.v1.EmailSuccess

object MockEmailClientSuccess {

  def apply(): Behavior[EmailClientCommand] = {
    Behaviors.receiveMessage[EmailClientCommand] {

      case SendEmail(_, _, _, replyTo) =>
        replyTo ! EmailSuccess
        Behaviors.same
    }
  }
}
