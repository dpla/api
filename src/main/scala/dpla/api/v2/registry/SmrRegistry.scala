package dpla.api.v2.registry

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext


/**
 * Handles the control flow for processing an SMR request from Routes.
 */
object SmrRegistry extends SmrRegistryBehavior {
  // TODO override spawning of children
}
