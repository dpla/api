package dpla.api.v2.registry

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.api.v2.smr.SmrRequestHandler
import dpla.api.v2.smr.SmrProtocol.SmrCommand


/**
 * Handles the control flow for processing an SMR request from Routes.
 */
object SmrRegistry extends SmrRegistryBehavior {

  override def spawnSmrRequestHandler(context: ActorContext[SmrRegistryCommand]):
    ActorRef[SmrCommand] =

    context.spawn(SmrRequestHandler(), "SMR Request Handler")
}
