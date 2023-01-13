package dpla.api.v2.smr

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import dpla.api.v2.smr.SmrProtocol.{ArchivePost, IntermediateSmrResult, RawSmrParams, SmrCommand}

trait SmrRequestHandlerBehavior {

  // Abstract methods
  def spawnS3Client(
                     context: ActorContext[SmrCommand]
                   ): ActorRef[IntermediateSmrResult]

  def spawnQueryBuilder(
                         context: ActorContext[SmrCommand],
                         s3Client: ActorRef[IntermediateSmrResult]
                       ): ActorRef[IntermediateSmrResult]

  def spawnParamValidator(
                           context: ActorContext[SmrCommand],
                           queryBuilder: ActorRef[IntermediateSmrResult]
                         ): ActorRef[IntermediateSmrResult]

  def apply(): Behavior[SmrCommand] = {

    Behaviors.setup[SmrCommand] { context =>

      // Spawn children.
      val s3Client: ActorRef[IntermediateSmrResult] =
        spawnS3Client(context)

      val queryBuilder: ActorRef[IntermediateSmrResult] =
        spawnQueryBuilder(context, s3Client)

      val paramValidator: ActorRef[IntermediateSmrResult] =
        spawnParamValidator(context, queryBuilder)

      Behaviors.receiveMessage[SmrCommand] {

        case ArchivePost(params, replyTo) =>
          paramValidator ! RawSmrParams(params, replyTo)
          Behaviors.same

        case _ =>
          Behaviors.unhandled
      }
    }
  }
}
