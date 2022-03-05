package dpla.ebookapi.v1.authentication

import akka.NotUsed
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import dpla.ebookapi.v1.authentication.AuthParamValidator.{AuthParamValidatorCommand, ValidateApiKey, ValidateEmail}
import dpla.ebookapi.v1.authentication.PostgresClient.PostgresClientCommand


// Command protocol
sealed trait AuthenticatorCommand

case class Authorize(
                      apiKey: String,
                      replyTo: ActorRef[AuthenticatorResponse]
                    ) extends AuthenticatorCommand

case class CreateAccount(
                          email: String,
                          replyTo: ActorRef[AuthenticatorResponse]
                        ) extends AuthenticatorCommand

// Response protocol
sealed trait AuthenticatorResponse

case object Authorized extends AuthenticatorResponse
case object NotAuthorized extends AuthenticatorResponse

case class AccountCreated(account: Account) extends AuthenticatorResponse
case class ExistingAccount(account: Account)   extends AuthenticatorResponse
case object InvalidEmail extends AuthenticatorResponse

case object AuthenticatorFailure extends AuthenticatorResponse

trait AuthenticatorBehavior {

  // Abstract methods
  def spawnPostgresClient(
                           context: ActorContext[AuthenticatorCommand]
                         ): ActorRef[PostgresClientCommand]

  def apply(): Behavior[AuthenticatorCommand] = {

    Behaviors.setup[AuthenticatorCommand] { context =>

      // Spawn children.
      val postgresClient: ActorRef[PostgresClientCommand] =
        spawnPostgresClient(context)

      val paramValidator: ActorRef[AuthParamValidatorCommand] =
        context.spawn(AuthParamValidator(postgresClient), "AuthParamValidator")

      Behaviors.receiveMessage[AuthenticatorCommand] {

        case Authorize(key, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processAuthorize(key, replyTo, paramValidator)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case CreateAccount(email, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processCreateAccount(email, replyTo, paramValidator)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same
      }
    }
  }

  def processAuthorize(
                        apiKey: String,
                        replyTo: ActorRef[AuthenticatorResponse],
                        paramValidator: ActorRef[AuthParamValidatorCommand]
                      ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      // Send initial message to AuthParamValidator
      // AuthParamValidator will forward valid key to PostgresClient
      paramValidator ! ValidateApiKey(apiKey, context.self, context.self)

      Behaviors.receiveMessage {

        /**
         * Possible responses from AuthParamValidator
         */

        case InvalidAuthParam =>
          replyTo ! NotAuthorized
          Behaviors.stopped

        /**
         * Possible responses from PostgresClient
         */

        case UserFound(account) =>
          if (account.enabled.getOrElse(false)) {
            replyTo ! Authorized
          } else {
            replyTo ! NotAuthorized
          }
          Behaviors.stopped

        case UserNotFound =>
          replyTo ! NotAuthorized
          Behaviors.stopped

        case PostgresError =>
          replyTo ! AuthenticatorFailure
          Behaviors.stopped
      }
    }.narrow[NotUsed]
  }

  def processCreateAccount(
                            email: String,
                            replyTo: ActorRef[AuthenticatorResponse],
                            paramValidator: ActorRef[AuthParamValidatorCommand]
                          ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      // Send initial message to AuthParamValidator
      // AuthParamValidator will forward valid key to PostgresClient
      paramValidator ! ValidateEmail(email, context.self, context.self)

      Behaviors.receiveMessage {

        /**
         * Possible responses from AuthParamValidator
         */

        case InvalidAuthParam =>
          replyTo ! InvalidEmail
          Behaviors.stopped

        /**
         * Possible responses from PostgresClient
         */

        case UserCreated(account) =>
          replyTo ! AccountCreated(account)
          Behaviors.stopped

        case UserFound(account) =>
          replyTo ! ExistingAccount(account)
          Behaviors.stopped

        case PostgresError =>
          replyTo ! AuthenticatorFailure
          Behaviors.stopped
      }
    }.narrow[NotUsed]
  }
}
