package dpla.api.v2.smr

import akka.NotUsed
import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.alpakka.s3.{MultipartUploadResult, S3Headers}
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import dpla.api.v2.smr.S3ResponseHandler._
import dpla.api.v2.smr.SmrProtocol._

import scala.concurrent.Future


/**
 * Handles interactions with S3.
 */

object S3Client {

  def apply(
             bucket: String
           ): Behavior[IntermediateSmrResult] = {

    Behaviors.setup { context =>

      val responseHandler: ActorRef[S3ResponseHandlerCommand] =
        context.spawn(
          S3ResponseHandler(),
          "S3ResponseHandler"
        )

      Behaviors.receiveMessage[IntermediateSmrResult] {

        case SmrUpload(data, key, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor = processSmrQuery(data, bucket, key, replyTo,
            responseHandler)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case _ =>
          Behaviors.unhandled
      }
    }
  }

  def processSmrQuery(
                       data: String,
                       bucket: String,
                       key: String,
                       replyTo: ActorRef[SmrResponse],
                       responseHandler: ActorRef[S3ResponseHandlerCommand]
                     ): Behavior[S3Response] = {

    Behaviors.setup { context =>

      implicit val system: ActorSystem[Nothing] = context.system

      val file: Source[ByteString, NotUsed] = Source.single(ByteString(data))
      val s3Sink: Sink[ByteString, Future[MultipartUploadResult]] =
        S3.multipartUploadWithHeaders(
          bucket = bucket,
          key = key,
          s3Headers = S3Headers.empty
        )
      val futureResp: Future[MultipartUploadResult] = file.runWith(s3Sink)

      // Send response future to S3ResponseHandler
      responseHandler ! ProcessS3Response(futureResp, context.self)

      Behaviors.receiveMessage[S3Response] {

        case S3UploadSuccess =>
          context.log.infoN(
            "S3 file successfully upload to {}/{}: {}",
            bucket, key, data
          )
          replyTo ! SmrSuccess
          Behaviors.stopped

        case S3UploadFailure(_) =>
          // TODO any response or retries based on error type?
          context.log.infoN(
            "S3 file failed to upload to {}/{}: {}",
            bucket, key, data
          )
          replyTo ! SmrFailure
          Behaviors.stopped
      }
    }

  }
}
