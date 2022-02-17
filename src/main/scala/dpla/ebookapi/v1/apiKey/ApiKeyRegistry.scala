package dpla.ebookapi.v1.apiKey

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import dpla.ebookapi.v1.{AccountCreated, AccountFound, AccountNotFound, InternalFailure, InvalidParams, ParamValidator, PostgresError, RegistryResponse, UserAccount, ValidEmail, ValidationFailure}
import dpla.ebookapi.v1.PostgresClient.{CreateAccount, FindAccountByEmail, PostgresClientCommand}
import ParamValidator.{ValidateEmail, ValidationCommand}

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

      Behaviors.receiveMessage[ApiKeyRegistryCommand] {

        case CreateApiKey(email, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processCreateApiKey(email, replyTo, paramValidator, postgresClient)
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
                           postgresClient: ActorRef[PostgresClientCommand]
                         ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

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
          postgresClient ! FindAccountByEmail(email, context.self)
          Behaviors.same

        case InvalidParams(message) =>
          replyTo ! ValidationFailure(message)
          Behaviors.stopped

        /**
         * Possible responses from PostgresClient.
         * Send either new API key or error back to Routes.
         */
        case AccountFound(account) =>
          // TODO email reminder
          replyTo ! ExistingApiKey(account.email)
          Behaviors.stopped

        case AccountNotFound =>
          postgresClient ! CreateAccount(email, context.self)
          Behaviors.same

        case AccountCreated(account) =>
          // TODO email new key
          replyTo ! NewApiKey(account.email)
          Behaviors.stopped

        case PostgresError =>
          replyTo ! InternalFailure
          Behaviors.stopped
      }
    }.narrow[NotUsed]
  }
}
