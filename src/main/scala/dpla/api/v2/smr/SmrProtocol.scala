package dpla.api.v2.smr

import akka.actor.typed.ActorRef
import spray.json.JsValue

object SmrProtocol {

  /** Public command protocol */
  sealed trait SmrCommand

  final case class ArchivePost(
                                service: String,
                                post: String,
                                user: String,
                                replyTo: ActorRef[SmrCommand]
                              ) extends SmrCommand

  /** Public response protocol */
  sealed trait SmrResponse

  final case class InvalidSmrParams(message: String) extends SmrResponse
  final case object SmrSuccess extends SmrResponse
  final case object SmrFailure extends SmrResponse

  /**
   * Internal command protocol.
   * Used by actors within the package to communicate with one another.
   */
  private[smr] trait IntermediateSmrResult

  private[smr] final case class RawParams(
                                            params: Map[String, String],
                                            replyTo: ActorRef[SmrResponse]
                                          ) extends IntermediateSmrResult

  private[smr] final case class ValidParams(
                                             params: SmrParams,
                                             replyTo: ActorRef[SmrResponse]
                                           ) extends IntermediateSmrResult

  private[smr] final case class SmrQuery(
                                          query: JsValue,
                                          replyTo: ActorRef[SmrResponse]
                                        ) extends IntermediateSmrResult
}
