package dpla.ebookapi.v1.apiKey

import akka.NotUsed
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import dpla.ebookapi.v1.EmailClient.{EmailClientCommand, SendEmail}
import dpla.ebookapi.v1.authentication.{Account, AccountCreated, AuthenticatorCommand, AuthenticatorFailure, CreateAccount, ExistingAccount, InvalidAuthParam, InvalidEmail}
import dpla.ebookapi.v1.{EmailFailure, EmailSuccess, InternalFailure, RegistryResponse, ValidationFailure}

// Command protocol
sealed trait ApiKeyRegistryCommand

final case class CreateApiKey(
                               email: String,
                               replyTo: ActorRef[RegistryResponse]
                             ) extends ApiKeyRegistryCommand

// Response protocol
final case class NewApiKey(
                            email: String
                          ) extends RegistryResponse

final case class ExistingApiKey(
                                 email: String
                               ) extends RegistryResponse

final case class DisabledApiKey(
                                 email: String
                               ) extends RegistryResponse


trait ApiKeyRegistryBehavior {

  // Abstract methods
  def spawnAuthenticator(context: ActorContext[ApiKeyRegistryCommand]):
    ActorRef[AuthenticatorCommand]

  def spawnEmailClient(context: ActorContext[ApiKeyRegistryCommand]):
    ActorRef[EmailClientCommand]

  def apply(): Behavior[ApiKeyRegistryCommand] = {

    Behaviors.setup[ApiKeyRegistryCommand] { context =>

      // Spawn children.
      val authenticator: ActorRef[AuthenticatorCommand] =
        spawnAuthenticator(context)

      val emailClient: ActorRef[EmailClientCommand] =
        spawnEmailClient(context)

      Behaviors.receiveMessage[ApiKeyRegistryCommand] {

        case CreateApiKey(email, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processCreateApiKey(email, replyTo, authenticator, emailClient)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same
      }
    }
  }

  /**
   * Per session actor behavior for handling request for a new api key.
   * The session actor has its own internal state
   * and its own ActorRef for sending/receiving messages.
   */
  def processCreateApiKey(
                           email: String,
                           replyTo: ActorRef[RegistryResponse],
                           authenticator: ActorRef[AuthenticatorCommand],
                           emailClient: ActorRef[EmailClientCommand]
                         ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      var userAccount: Option[Account] = None
      var accountAlreadyExists: Boolean = false

      // Send initial message to Authenticator.
      authenticator ! CreateAccount(email, context.self)

      Behaviors.receiveMessage {

        /**
         * Possible responses from Authenticator.
         * Either send an email request to EmailClient
         * or an error back to Routes.
         */

        case InvalidEmail =>
          replyTo ! ValidationFailure("Invalid email address")
          Behaviors.stopped

        case ExistingAccount(account) =>
          if (account.enabled.getOrElse(true)) {
            userAccount = Some(account)
            accountAlreadyExists = true
            emailClient ! SendEmail(
              account.email,
              "Your existing DPLA API key",
              "Your existing DPLA API key is " + account.key,
              context.self
            )
            Behaviors.same
          } else {
            replyTo ! DisabledApiKey(account.email)
            Behaviors.stopped
          }

        case AccountCreated(account) =>
          userAccount = Some(account)
          accountAlreadyExists = false
          emailClient ! SendEmail(
            account.email,
            "Your new DPLA API key",
            "Your new DPLA API key is " + account.key,
            context.self
          )
          Behaviors.same

        case AuthenticatorFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        /**
         * Possible responses from EmailClient.
         * Send either a final message or an error back to Routes.
         */

        case EmailSuccess =>
          userAccount match {
            case Some(account) =>
              if (accountAlreadyExists)
                replyTo ! ExistingApiKey(account.email)
              else
                replyTo ! NewApiKey(account.email)
            case None =>
              // This shouldn't happen
              context.log.error("Missing account.")
              replyTo ! InternalFailure
          }
          Behaviors.stopped

        case EmailFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped
      }
    }.narrow[NotUsed]
  }
}
