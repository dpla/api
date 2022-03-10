package dpla.ebookapi.mocks

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.authentication.PostgresClient._
import dpla.ebookapi.v1.authentication
import dpla.ebookapi.v1.authentication.UserNotFound

object MockPostgresClientUnsuccessful {

  def apply(): Behavior[PostgresClientCommand] = {
    Behaviors.receiveMessage[PostgresClientCommand] {

      case FindUserByKey(_, replyTo) =>
        replyTo ! UserNotFound
        Behaviors.same

      case CreateUser(_, replyTo) =>
        replyTo ! authentication.PostgresError
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}
