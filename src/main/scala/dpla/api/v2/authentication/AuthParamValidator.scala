package dpla.api.v2.authentication

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import dpla.api.v2.authentication.AuthProtocol.{ValidEmail, ValidApiKey, IntermediateAuthResult, InvalidApiKey, InvalidEmail, RawApiKey, RawEmail}
import org.apache.commons.validator.routines.EmailValidator


/**
 * Validates user-submitted authentication parameters.
 * Bad actors may use invalid search params to try and hack the system, so they
 * are logged as warnings.
 */
object AuthParamValidator {

  def apply(
             nextPhase: ActorRef[IntermediateAuthResult]
           ): Behavior[IntermediateAuthResult] = {

    Behaviors.setup { context =>

      Behaviors.receiveMessage {

        case RawApiKey(apiKey, replyTo) =>
          val isValid = isValidApiKey(apiKey)
          if (!isValid) {
            context.log.warn("Invalid API key param: {}", apiKey)
            replyTo ! InvalidApiKey
          }
          else {
            nextPhase ! ValidApiKey(apiKey, replyTo)
          }
          Behaviors.same

        case RawEmail(email, replyTo) =>
          val isValid = isValidEmail(email)
          if (!isValid) {
            context.log.warn("Invalid email param: {}", email)
            replyTo ! InvalidEmail
          }
          else {
            nextPhase ! ValidEmail(email, replyTo)
          }
          Behaviors.same

        case _ =>
          Behaviors.unhandled
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
