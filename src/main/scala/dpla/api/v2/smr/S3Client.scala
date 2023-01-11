package dpla.api.v2.smr

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import dpla.api.v2.smr.SmrProtocol.{IntermediateSmrResult, SmrQuery}

/**
 * Handles interactions with S3.
 */

object S3Client {

  def apply(
             endpoint: String
           ): Behavior[IntermediateSmrResult] = {

    Behaviors.setup { context =>

      Behaviors.receiveMessage[IntermediateSmrResult] {

        case SmrQuery(query, replyTo) =>
          // TODO
          Behaviors.same

        case _ =>
          Behaviors.unhandled
      }
    }
  }
}
