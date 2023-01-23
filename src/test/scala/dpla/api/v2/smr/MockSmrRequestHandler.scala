package dpla.api.v2.smr

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.api.v2.smr.SmrProtocol.{IntermediateSmrResult, SmrCommand}

object MockSmrRequestHandler {

  def apply(
             testKit: ActorTestKit,
             s3Client: Option[ActorRef[IntermediateSmrResult]] = None
           ): ActorRef[SmrCommand] = {

    object Mock extends SmrRequestHandlerBehavior {


      override def spawnS3Client(
                                  context: ActorContext[SmrCommand]
                                ): ActorRef[IntermediateSmrResult] =
        s3Client.getOrElse(
          context.spawnAnonymous(S3Client("fake-bucket"))
        )

      override def spawnDataUploadBuilder(
                                           context: ActorContext[SmrCommand],
                                           s3Client: ActorRef[IntermediateSmrResult]
                                         ): ActorRef[IntermediateSmrResult] =

        context.spawnAnonymous(SmrDataUploadBuilder(s3Client))

      override def spawnParamValidator(
                                        context: ActorContext[SmrCommand],
                                        dataUploadBuilder: ActorRef[IntermediateSmrResult]
                                      ): ActorRef[IntermediateSmrResult] =

        context.spawnAnonymous(SmrParamValidator(dataUploadBuilder))
    }

    testKit.spawn(Mock())
  }
}
