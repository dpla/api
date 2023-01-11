package dpla.api.v2.smr

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import dpla.api.v2.smr.SmrProtocol.{IntermediateSmrResult, ValidParams}

/**
 * Composes S3 queries from user-submitted parameters.
 */
object SmrQueryBuilder {

  def apply(
             nextPhase: ActorRef[IntermediateSmrResult]
           ): Behavior[IntermediateSmrResult] = {

    Behaviors.receiveMessage[IntermediateSmrResult] {

      case ValidParams(params, replyTo) =>
        // TODO
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}
