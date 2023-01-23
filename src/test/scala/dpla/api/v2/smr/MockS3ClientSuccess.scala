package dpla.api.v2.smr

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import dpla.api.v2.smr.SmrProtocol.{IntermediateSmrResult, SmrSuccess, SmrUpload}


object MockS3ClientSuccess {

  def apply(): Behavior[IntermediateSmrResult] = {
    Behaviors.receiveMessage[IntermediateSmrResult] {

      case SmrUpload(_, _, replyTo) =>
        replyTo ! SmrSuccess
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}
