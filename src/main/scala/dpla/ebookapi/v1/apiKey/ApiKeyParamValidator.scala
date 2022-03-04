package dpla.ebookapi.v1.apiKey

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import dpla.ebookapi.v1.{InvalidParams, ValidationResponse}
import org.apache.commons.validator.routines.EmailValidator


/**
 * Validates user-submitted parameters. Provides default values when appropriate.
 * Bad actors may use invalid search params to try and hack the system, so they
 * are logged as warnings.
 */

final case class ValidEmail(
                             email: String
                           ) extends ValidationResponse

object ApiKeyParamValidator {

  sealed trait ApiKeyValidationCommand

  final case class ValidateEmail(
                                  email: String,
                                  replyTo: ActorRef[ValidationResponse]
                                ) extends ApiKeyValidationCommand

  def apply(): Behavior[ApiKeyValidationCommand] = {
    Behaviors.setup { context =>
      Behaviors.receiveMessage {

        case ValidateEmail(email, replyTo) =>
          val response = getValidEmail(email)

          // Log warning for invalid params.
          response match {
            case InvalidParams(msg) =>
              context.log.warn(msg)

            case _ => // noop
          }

          replyTo ! response
          Behaviors.same
      }
    }
  }

  /**
   * Validates email format using the Apache Commons validator.
   * Limits length to 100 characters to be in compliance with database.
   */
  private def getValidEmail(email: String): ValidationResponse =
    if (EmailValidator.getInstance.isValid(email) && email.length <= 100)
      ValidEmail(email)
    else
      InvalidParams(s"$email is not a valid email address.")
}
