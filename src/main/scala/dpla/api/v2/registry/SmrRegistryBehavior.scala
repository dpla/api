package dpla.api.v2.registry

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import dpla.api.v2.authentication.AuthProtocol._
import dpla.api.v2.registry.RegistryProtocol._
import dpla.api.v2.smr.SmrProtocol._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}


// command protocol

sealed trait SmrRegistryCommand

final case class RegisterSmrArchiveRequest(
                                            apiKey: Option[String],
                                            rawParams: SmrArchiveRequest,
                                            host: String,
                                            path: String,
                                            replyTo: ActorRef[RegistryResponse]
                                          ) extends SmrRegistryCommand

final case class SmrArchiveRequest(
                                    service: Option[String],
                                    post: Option[String],
                                    user: Option[String]
                                  )
// unmarshaller
object SmrArchiveRequestJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val PortfolioFormats: RootJsonFormat[SmrArchiveRequest] =
    jsonFormat3(SmrArchiveRequest)
}

// response protocol

case object SmrArchiveSuccess extends RegistryResponse

trait SmrRegistryBehavior {

  // abstract method
  def spawnSmrRequestHandler(context: ActorContext[SmrRegistryCommand]): ActorRef[SmrCommand]

  def apply(
             authenticator: ActorRef[AuthenticationCommand]
           ): Behavior[SmrRegistryCommand] = {

    Behaviors.setup[SmrRegistryCommand] { context =>

      // Spawn children.
      val smrRequestHandler: ActorRef[SmrCommand] =
        spawnSmrRequestHandler(context)

      Behaviors.receiveMessage[SmrRegistryCommand] {

        case RegisterSmrArchiveRequest(apiKey, request, host, path, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processSmrArchiveRequest(apiKey, request, replyTo, authenticator,
              smrRequestHandler)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same
      }
    }
  }

  def processSmrArchiveRequest(
                                apiKey: Option[String],
                                request: SmrArchiveRequest,
                                replyTo: ActorRef[RegistryResponse],
                                authenticator: ActorRef[AuthenticationCommand],
                                smrRequestHandler: ActorRef[SmrCommand]
                              ): Behavior[NotUsed] = {

    Behaviors.setup[AnyRef] { context =>

      // Send initial message to authenticator.
      authenticator ! FindAccountByKey(apiKey, context.self)

      Behaviors.receiveMessage {

        /**
         * Possible responses from Authenticator.
         */
        case AccountFound(_) =>
          smrRequestHandler ! ArchivePost(request, context.self)
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

        case InvalidSmrParams(msg) =>
          replyTo ! ValidationFailure(msg)
          Behaviors.stopped

        case SmrSuccess =>
          replyTo ! SmrArchiveSuccess
          Behaviors.stopped

        case SmrFailure =>
          replyTo ! InternalFailure
          Behaviors.stopped

        case _ =>
          Behaviors.unhandled
      }
    }.narrow[NotUsed]
  }
}
