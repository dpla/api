package dpla.api.v2.smr

import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.DateTime
import dpla.api.v2.smr.SmrProtocol.{IntermediateSmrResult, InvalidSmrParams, RawParams, ValidParams}

import scala.util.{Failure, Success, Try}

/** Case classes for representing valid smr parameters */

private[smr] case class SmrParams(
                                   post: String,
                                   service: String,
                                   timestamp: String,
                                   user: String
                                 )
/**
 * Validates user-submitted SMR parameters.
 * Bad actors may use invalid search params to try and hack the system, so they
 * are logged as warnings.
 */
object SmrParamValidator {

  def apply(
             nextPhase: ActorRef[IntermediateSmrResult]
           ): Behavior[IntermediateSmrResult] = {

    Behaviors.setup { context =>

      Behaviors.receiveMessage {

        case RawParams(rawParams, replyTo) =>
          getSmrParams(rawParams) match {
            case Success(smrParams) =>
              nextPhase ! ValidParams(smrParams, replyTo)
            case Failure(e) =>
              context.log.warn2(
                "Invalid smr params: '{}' for params '{}'",
                e.getMessage,
                rawParams.map { case(key, value) => s"$key: $value"}.mkString(", ")
              )
              replyTo ! InvalidSmrParams(e.getMessage)
          }
          Behaviors.same

        case _ =>
          Behaviors.unhandled
      }
    }
  }

  private case class ValidationException(
                                          private val message: String = ""
                                        ) extends Exception(message)

  private val validServices: Seq[String] = Seq("tiktok")

  // These user parameters are valid.
  private val acceptedSmrParams = Seq(
    "service",
    "post",
    "user"
  )

  private def getSmrParams(rawParams: Map[String, String]): Try[SmrParams] = Try {
    // Check for unrecognized params
    val unrecognized = rawParams.keys.toSeq diff acceptedSmrParams

    if (unrecognized.nonEmpty)
      throw ValidationException(
        "Unrecognized parameter: " + unrecognized.mkString(", ")
      )
    else {
      // Check for valid search params

      def missingParamException(param: String) =
        ValidationException("Missing required parameter: $param")

      val rawService: String = rawParams
        .getOrElse("service", throw missingParamException("service"))

      val rawPost: String = rawParams
        .getOrElse("post", throw missingParamException("post"))

      val rawUser: String = rawParams
        .getOrElse("user", throw missingParamException("user"))

      SmrParams(
        service = validService(rawService),
        post = validPost(rawPost),
        user = validUser(rawUser),
        timestamp = DateTime.now.toIsoDateTimeString()
      )
    }
  }

  private def validService(service: String): String = {
    val rule = "service must be one of: " + validServices.mkString(", ")

    if (validServices.contains(service)) service
    else throw ValidationException(rule)
  }

  // TODO enforce actual rule, this is a stand-in
  def validPost(post: String): String = {
    val rule = "post must be a String comprised of letters, numbers, and " +
      "hyphens between 1 and 32 characters long"

    if (post.length < 1 || post.length > 32) throw ValidationException(rule)
    else if (post.matches("[a-zA-Z0-9-]*")) post
    else throw ValidationException(rule)
  }

  // TODO enforce actual rule, this is a stand-in
  def validUser(user: String): String = {
    val rule = "user must be a String comprised of letters, numbers, and " +
      "hyphens between 1 and 32 characters long"

    if (user.length < 1 || user.length > 32) throw ValidationException(rule)
    else if (user.matches("[a-zA-Z0-9-]*")) user
    else throw ValidationException(rule)
  }
}