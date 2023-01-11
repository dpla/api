package dpla.api.v2.registry

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import dpla.api.v2.authentication.AuthProtocol.{AccountFound, AccountNotFound, AuthenticationCommand, AuthenticationFailure, FindAccountByKey, InvalidApiKey}
import dpla.api.v2.registry.RegistryProtocol.{ForbiddenFailure, InternalFailure, RegistryResponse}

// command protocol

sealed trait SmrRegistryCommand

final case class SmrArchiveRequest(
                                    apiKey: Option[String],
                                    rawParams: Map[String, String],
                                    host: String,
                                    path: String,
                                    replyTo: ActorRef[RegistryResponse]
                                  ) extends SmrRegistryCommand

// response protocol

case object SmrArchiveSuccess

trait SmrRegistryBehavior {

  def apply(
             authenticator: ActorRef[AuthenticationCommand]
           ): Behavior[SmrRegistryCommand] = {

    Behaviors.setup[SmrRegistryCommand] { context =>

      // TODO Spawn children.
      // Param validator
      // S3 client

      Behaviors.receiveMessage[SmrRegistryCommand] {

        case SmrArchiveRequest(apiKey, rawParams, host, path, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processSmrArchiveRequest(apiKey, rawParams, replyTo, authenticator)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same
      }
    }
  }

  def processSmrArchiveRequest(
                                apiKey: Option[String],
                                rawParams: Map[String, String],
                                replyTo: ActorRef[RegistryResponse],
                                authenticator: ActorRef[AuthenticationCommand]
                              ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      // Send initial message to authenticator.
      authenticator ! FindAccountByKey(apiKey, context.self)

      Behaviors.receiveMessage {

        /**
         * Possible responses from Authenticator.
         */
        case AccountFound(account) =>
          // TODO
          Behaviors.same

        case AccountNotFound =>
          replyTo ! ForbiddenFailure
          Behaviors.stopped

        case InvalidApiKey =>
          replyTo ! ForbiddenFailure
          Behaviors.stopped

        case AuthenticationFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        /**
         * Possible responses from smr request handler.
         */

        /**
         * Possible responses from s3 client.
         */

        case _ =>
          Behaviors.unhandled
      }
    }.narrow[NotUsed]
  }
}
