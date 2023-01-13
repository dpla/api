package dpla.api.v2.smr

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import spray.json._
import dpla.api.v2.smr.SmrProtocol.{IntermediateSmrResult, SmrUpload, ValidSmrParams}

/**
 * Composes JSON data to be uploaded to S3 from user-submitted parameters.
 */
object SmrDataUploadBuilder extends DefaultJsonProtocol {

  def apply(
             nextPhase: ActorRef[IntermediateSmrResult]
           ): Behavior[IntermediateSmrResult] = {

    Behaviors.receiveMessage[IntermediateSmrResult] {

      case ValidSmrParams(smrParams, replyTo) =>
        val key: String = smrParams.timestamp.clicks.toString + ".json"

        val data: String = JsObject(
          "service" -> smrParams.service.toJson,
          "post" -> smrParams.post.toJson,
          "user" -> smrParams.user.toJson,
          "timestamp" -> smrParams.timestamp.toIsoDateTimeString.toJson
        ).toString

        nextPhase ! SmrUpload(data, key, replyTo)
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}
