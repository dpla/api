package dpla.api.v2.smr

import akka.NotUsed
import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.alpakka.s3.MultipartUploadResult
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import dpla.api.v2.smr.S3ResponseHandler.{ProcessS3Response, S3ResponseHandlerCommand}
import dpla.api.v2.smr.SmrProtocol.{IntermediateSmrResult, SmrFailure, SmrResponse, SmrSuccess, SmrUpload}
//import com.amazonaws.AmazonServiceException
//import com.amazonaws.regions.Regions
//import com.amazonaws.services.s3.model.{MultipartUpload, ObjectMetadata, PutObjectRequest, PutObjectResult}
//import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
//import com.amazonaws.services.s3.transfer._

//import java.io.ByteArrayInputStream
import scala.concurrent.Future
//import scala.util.Try


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

//      AmazonS3ClientBuilder.standard.build

//      val s3 = AmazonS3ClientBuilder.standard.withRegion(Regions.DEFAULT_REGION).build
//      val in = new ByteArrayInputStream(data.getBytes("utf-8"))
//      val result: Try[PutObjectResult] = Try {
//        s3.putObject(new PutObjectRequest(bucket, key, in, new ObjectMetadata))
//      }
//
//      val transferManager = TransferManagerBuilder.standard.build
//      transferManager.upload(bucket, key, in, new ObjectMetadata).waitForUploadResult()

      // Upload file to S3
      val file: Source[ByteString, NotUsed] = Source.single(ByteString(data))
      val s3Sink: Sink[ByteString, Future[MultipartUploadResult]] =
        S3.multipartUpload(bucket, key)
      val futureResp: Future[MultipartUploadResult] = file.runWith(s3Sink)

      context.log.infoN(
        "S3 file upload to {}/{}: {}",
        bucket, key, data
      )

      // Send response future to ElasticSearchResponseHandler
      responseHandler ! ProcessS3Response(futureResp, context.self)

      Behaviors.receiveMessage[S3Response] {

        case S3UploadSuccess =>
          replyTo ! SmrSuccess
          Behaviors.stopped

        case S3UploadFailure(_) =>
          // TODO any response or retries based on error type?
          replyTo ! SmrFailure
          Behaviors.stopped
      }
    }

  }
}
