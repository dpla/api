package dpla.api.v2.smr

import akka.actor.typed.ActorRef
import dpla.api.v2.registry.SmrArchiveRequest

object SmrProtocol {

  /** Public command protocol */
  sealed trait SmrCommand

  final case class ArchivePost(
                                request: SmrArchiveRequest,
                                replyTo: ActorRef[SmrResponse]
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

  private[smr] final case class RawSmrParams(
                                              request: SmrArchiveRequest,
                                              replyTo: ActorRef[SmrResponse]
                                            ) extends IntermediateSmrResult

  private[smr] final case class ValidSmrParams(
                                             params: SmrParams,
                                             replyTo: ActorRef[SmrResponse]
                                           ) extends IntermediateSmrResult

  private[smr] final case class SmrUpload(
                                           data: String,
                                           key: String,
                                           replyTo: ActorRef[SmrResponse]
                                         ) extends IntermediateSmrResult
}
