package dpla.api.v2.smr

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.alpakka.s3.MultipartUploadResult

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Processes response streams from S3.
 */

sealed trait S3Response

case object S3UploadSuccess extends S3Response

final case class S3UploadFailure(
                                  error: Throwable
                                ) extends S3Response

object S3ResponseHandler {

  sealed trait S3ResponseHandlerCommand

  final case class ProcessS3Response(
                                      futureUploadResponse: Future[MultipartUploadResult],
                                      replyTo: ActorRef[S3Response]
                                    ) extends S3ResponseHandlerCommand

  private final case class ReturnFinalResponse(
                                                response: S3Response,
                                                replyTo: ActorRef[S3Response],
                                                error: Option[Throwable] = None
                                              ) extends S3ResponseHandlerCommand

  def apply(): Behavior[S3ResponseHandlerCommand] = {
    Behaviors.setup { context =>
      Behaviors.receiveMessage[S3ResponseHandlerCommand] {

        case ProcessS3Response(futureUploadResponse, replyTo) =>
          // Map the Future value to a message, handled by this actor.
          context.pipeToSelf(futureUploadResponse) {
            case Success(_) =>
              ReturnFinalResponse(S3UploadSuccess, replyTo)
            case Failure(e) =>
              ReturnFinalResponse(S3UploadFailure(e), replyTo, Some(e))
          }
          Behaviors.same

        case ReturnFinalResponse(response, replyTo, error) =>
          // Log error if there is one
          error match {
            case Some(e) =>
              context.log.error(
                "S3 upload failed: ", e
              )
            case None => // no-op
          }
          // Send fully processed reply to original requester.
          replyTo ! response
          Behaviors.same
      }
    }
  }
}
