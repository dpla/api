package dpla.ebookapi.v1.authentication

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import dpla.ebookapi.v1.authentication.AuthProtocol.{AuthenticationCommand, CreateAccount, FindAccountByKey, IntermediateAuthResult, RawApiKey, RawEmail}


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

        case FindAccountByKey(key, replyTo) =>
          paramValidator ! RawApiKey(key, replyTo)
          Behaviors.same

        case CreateAccount(email, replyTo) =>
          paramValidator ! RawEmail(email, replyTo)
          Behaviors.same
      }
    }
  }
}
