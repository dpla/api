package dpla.ebookapi.v1.authentication

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import dpla.ebookapi.v1.authentication.PostgresClient.{CreateUser, FindUserByKey, PostgresClientCommand}
import org.apache.commons.validator.routines.EmailValidator


/**
 * Validates user-submitted authentication parameters.
 * Bad actors may use invalid search params to try and hack the system, so they
 * are logged as warnings.
 */

sealed trait AuthParamValidatorResponse

case object InvalidAuthParam extends AuthParamValidatorResponse

object AuthParamValidator {

  sealed trait AuthParamValidatorCommand

  final case class ValidateApiKey(
                                   apiKey: String,
                                   forwardTo: ActorRef[PostgresClientResponse],
                                   replyTo: ActorRef[AuthParamValidatorResponse]
                                 ) extends AuthParamValidatorCommand

  final case class ValidateEmail(
                                  email: String,
                                  forwardTo: ActorRef[PostgresClientResponse],
                                  replyTo: ActorRef[AuthParamValidatorResponse]
                                ) extends AuthParamValidatorCommand

  def apply(
             postgresClient: ActorRef[PostgresClientCommand]
           ): Behavior[AuthParamValidatorCommand] = {

    Behaviors.setup { context =>

      Behaviors.receiveMessage {

        case ValidateApiKey(apiKey, forwardTo, replyTo) =>
          val isValid = isValidApiKey(apiKey)
          if (!isValid) {
            context.log.warn("Invalid API key param: {}", apiKey)
            // Send message back to Authenticator
            replyTo ! InvalidAuthParam
          }
          else {
            // Forward message to PostgresClient
            postgresClient ! FindUserByKey(apiKey, forwardTo)
          }
          Behaviors.same

        case ValidateEmail(email, forwardTo, replyTo) =>
          val isValid = isValidEmail(email)
          if (!isValid) {
            context.log.warn("Invalid email param: {}", email)
            // Send message back to Authenticator
            replyTo ! InvalidAuthParam
          }
          else {
            // Forward message to PostgresClient
            postgresClient ! CreateUser(email, forwardTo)
          }
          Behaviors.same
      }
    }
  }

  /**
   * Must be a String 32 characters long, comprised of letters and numbers
   */
  private def isValidApiKey(apiKey: String): Boolean = {
    if (apiKey.length != 32) false
    else if (apiKey.matches("[a-zA-Z0-9-]*")) true
    else false
  }

  /**
   * Validates email format using the Apache Commons validator.
   * Limits length to 100 characters to be in compliance with database.
   */
  private def isValidEmail(email: String): Boolean =
    if (EmailValidator.getInstance.isValid(email) && email.length <= 100) true
    else false
}
