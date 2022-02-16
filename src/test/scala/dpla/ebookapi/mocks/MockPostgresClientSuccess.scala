package dpla.ebookapi.mocks

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.PostgresClient.{CreateApiKey, FindApiKey, PostgresClientCommand}
import dpla.ebookapi.v1.{ApiKey, ApiKeyCreated, ApiKeyFound}

object MockPostgresClientSuccess {

  private val apiKey = ApiKey(
    key = "08e3918eeb8bf4469924f062072459a8",
    email = "x@example.org",
    staff = false,
    enabled = true
  )

  def apply(): Behavior[PostgresClientCommand] = {
    Behaviors.receiveMessage[PostgresClientCommand] {

      case FindApiKey(_, replyTo) =>
        replyTo ! ApiKeyFound(apiKey)
        Behaviors.same

      case CreateApiKey(_, replyTo) =>
        replyTo ! ApiKeyCreated(apiKey)
        Behaviors.same
    }
  }
}
