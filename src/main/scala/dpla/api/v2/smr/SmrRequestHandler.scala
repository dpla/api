package dpla.api.v2.smr
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.api.v2.smr.SmrProtocol._

/**
 * Handles requests to archive social media posts.
 */
object SmrRequestHandler extends SmrRequestHandlerBehavior {

  override def spawnS3Client(
                              context: ActorContext[SmrCommand]
                            ): ActorRef[IntermediateSmrResult] = {

    val endpoint: String = context.system.settings.config
      .getString("s3.smrBucket")
      .stripSuffix("/")

    context.spawn(S3Client(endpoint), "S3Client")
  }

  override def spawnQueryBuilder(
                                  context: ActorContext[SmrCommand],
                                  s3Client: ActorRef[IntermediateSmrResult]
                                ): ActorRef[IntermediateSmrResult] =

    context.spawn(SmrQueryBuilder(s3Client), "SmrQueryBuilder")

  override def spawnParamValidator(
                                    context: ActorContext[SmrCommand],
                                    queryBuilder: ActorRef[IntermediateSmrResult]
                                  ): ActorRef[IntermediateSmrResult] =

    context.spawn(SmrParamValidator(queryBuilder), "SmrParamValidator")
}
