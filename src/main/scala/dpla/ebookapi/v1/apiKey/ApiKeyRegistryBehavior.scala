package dpla.ebookapi.v1.apiKey

import akka.NotUsed
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import dpla.ebookapi.v1.EmailClient.{EmailClientCommand, SendEmail}
import dpla.ebookapi.v1.ParamValidator.{ValidateEmail, ValidationCommand}
import dpla.ebookapi.v1.PostgresClient.{CreateAccount, PostgresClientCommand}
import dpla.ebookapi.v1.{Account, AccountCreated, AccountFound, AccountNotFound, EmailFailure, EmailSuccess, InternalFailure, InvalidParams, PostgresError, RegistryResponse, ValidEmail, ValidationFailure}


final case class NewApiKey(
                            email: String
                          ) extends RegistryResponse

final case class ExistingApiKey(
                                 email: String
                               ) extends RegistryResponse

final case class DisabledApiKey(
                                 email: String
                               ) extends RegistryResponse

sealed trait ApiKeyRegistryCommand

final case class CreateApiKey(
                               email: String,
                               replyTo: ActorRef[RegistryResponse]
                             ) extends ApiKeyRegistryCommand

trait ApiKeyRegistryBehavior {

  def spawnParamValidator(context: ActorContext[ApiKeyRegistryCommand]):
    ActorRef[ValidationCommand]

  def spawnPostgresClient(context: ActorContext[ApiKeyRegistryCommand]):
    ActorRef[PostgresClientCommand]

  def spawnEmailClient(context: ActorContext[ApiKeyRegistryCommand]):
    ActorRef[EmailClientCommand]

  def apply(): Behavior[ApiKeyRegistryCommand] = {

    Behaviors.setup[ApiKeyRegistryCommand] { context =>

      // Spawn children.
      val paramValidator: ActorRef[ValidationCommand] =
        spawnParamValidator(context)

      val postgresClient: ActorRef[PostgresClientCommand] =
        spawnPostgresClient(context)

      val emailClient: ActorRef[EmailClientCommand] =
        spawnEmailClient(context)

      Behaviors.receiveMessage[ApiKeyRegistryCommand] {

        case CreateApiKey(email, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processCreateApiKey(email, replyTo, paramValidator, postgresClient, emailClient)
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
                           paramValidator: ActorRef[ValidationCommand],
                           postgresClient: ActorRef[PostgresClientCommand],
                           emailClient: ActorRef[EmailClientCommand]
                         ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      var userAccount: Option[Account] = None
      var accountAlreadyExists: Boolean = false

      // Send initial message to ParamValidator.
      paramValidator ! ValidateEmail(email, context.self)

      Behaviors.receiveMessage {

        /**
         * Possible responses from ParamValidator.
         * If email address is valid, send a message to PostgresClient to
         * create new account.
         * If email address is invalid, send an error message back to Routes.
         */
        case ValidEmail(email) =>
          postgresClient ! CreateAccount(email, context.self)
          Behaviors.same

        case InvalidParams(message) =>
          replyTo ! ValidationFailure(message)
          Behaviors.stopped

        /**
         * Possible responses from PostgresClient.
         * Either send an email request to EmailClient
         * or an error back to Routes.
         */
        case AccountFound(account) =>
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

        case PostgresError =>
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
