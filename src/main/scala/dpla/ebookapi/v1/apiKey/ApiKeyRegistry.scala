package dpla.ebookapi.v1.apiKey

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.{Account, AccountCreated, AccountFound, AccountNotFound, EmailClient, EmailFailure, EmailSuccess, InternalFailure, InvalidParams, ParamValidator, PostgresError, RegistryResponse, ValidEmail, ValidationFailure}
import dpla.ebookapi.v1.PostgresClient.{CreateAccount, PostgresClientCommand}
import ParamValidator.{ValidateEmail, ValidationCommand}
import dpla.ebookapi.v1.EmailClient.{EmailClientCommand, SendEmail}

/**
 * Handles the control flow for processing a request from Routes.
 * Its children include ParamValidator and session actors.
 * It also messages with PostgresClient.
 */
final case class NewApiKey(
                            email: String
                          ) extends RegistryResponse

final case class ExistingApiKey(
                                 email: String
                               ) extends RegistryResponse

final case class DisabledApiKey(
                                 email: String
                               ) extends RegistryResponse

object ApiKeyRegistry {

  sealed trait ApiKeyRegistryCommand

  final case class CreateApiKey(
                                 email: String,
                                 replyTo: ActorRef[RegistryResponse]
                               ) extends ApiKeyRegistryCommand

  def apply(
             postgresClient: ActorRef[PostgresClientCommand]
           ): Behavior[ApiKeyRegistryCommand] = {

    Behaviors.setup[ApiKeyRegistryCommand] { context =>

      // Spawn children.
      val paramValidator: ActorRef[ValidationCommand] =
        context.spawn(
          ParamValidator(),
          "ParamValidator"
        )

      val emailClient: ActorRef[EmailClientCommand] =
        context.spawn(
          EmailClient(),
          "EmailClient"
        )

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
          context.log.info("Received ValidEmail")
          postgresClient ! CreateAccount(email, context.self)
          Behaviors.same

        case InvalidParams(message) =>
          context.log.info("Received InvalidParams")
          replyTo ! ValidationFailure(message)
          Behaviors.stopped

        /**
         * Possible responses from PostgresClient.
         * Either send an email request to EmailClient
         * or an error back to Routes.
         */
        case AccountFound(account) =>
          context.log.info("Received AccountFound")
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
          context.log.info("Received AccountCreated")
          userAccount = Some(account)
          accountAlreadyExists = false
          emailClient ! SendEmail(
            account.email,
            "Your new DPLA API key",
            "Your new DPLA API key is " + account.key,
            context.self
          )
          Behaviors.same

        // TODO this should be handled in EmailClient
        case AccountNotFound =>
          // This should not happen.
          context.log.error(
            "Read-after-attempted-write failed in Postgres database"
          )
          replyTo ! InternalFailure
          Behaviors.stopped

        case PostgresError =>
          context.log.info("Received PostgreError")
          replyTo ! InternalFailure
          Behaviors.stopped

        /**
         * Possible responses from EmailClient.
         * Send either a final message or an error back to Routes.
         */

        case EmailSuccess =>
          context.log.info("Received EmailSuccess")
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
          context.log.info("Received EmailFailure")
          replyTo ! InternalFailure
          Behaviors.stopped
      }
    }.narrow[NotUsed]
  }
}
