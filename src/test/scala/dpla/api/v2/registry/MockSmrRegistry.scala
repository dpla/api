package dpla.api.v2.registry

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
import dpla.api.v2.smr.SmrProtocol.SmrCommand
import dpla.api.v2.smr.SmrRequestHandler

object MockSmrRegistry {

  def apply(
             testKit: ActorTestKit,
             authenticator: ActorRef[AuthenticationCommand],
             smrRequestHandler: Option[ActorRef[SmrCommand]] = None
           ): ActorRef[SmrRegistryCommand] = {

    object Mock extends SmrRegistryBehavior {

      override def spawnSmrRequestHandler (
                                            context: ActorContext[SmrRegistryCommand]
                                          ): ActorRef[SmrCommand] =

        smrRequestHandler.getOrElse(context.spawnAnonymous(SmrRequestHandler()))
    }

    testKit.spawn(Mock(authenticator))
  }
}
