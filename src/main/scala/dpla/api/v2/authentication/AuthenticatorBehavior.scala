package dpla.api.v2.authentication

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import dpla.api.v2.authentication.AuthProtocol.{AuthenticationCommand, CreateAccount, FindAccountByKey, IntermediateAuthResult, InvalidApiKey, RawApiKey, RawEmail}


trait AuthenticatorBehavior {

  // Abstract methods
  def spawnPostgresClient(
                           context: ActorContext[AuthenticationCommand]
                         ): ActorRef[IntermediateAuthResult]

  def apply(): Behavior[AuthenticationCommand] = {

    Behaviors.setup[AuthenticationCommand] { context =>

      // Spawn children.
      val postgresClient: ActorRef[IntermediateAuthResult] =
        spawnPostgresClient(context)

      val paramValidator: ActorRef[IntermediateAuthResult] =
        context.spawn(AuthParamValidator(postgresClient), "AuthParamValidator")

      Behaviors.receiveMessage[AuthenticationCommand] {

        case FindAccountByKey(keyOpt, replyTo) =>
          keyOpt match {
            case Some(key) =>
              paramValidator ! RawApiKey (key, replyTo)
            case None =>
              replyTo ! InvalidApiKey
          }
          Behaviors.same

        case CreateAccount(email, replyTo) =>
          paramValidator ! RawEmail(email, replyTo)
          Behaviors.same
      }
    }
  }
}
