package dpla.ebookapi.v1.ebooks

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.{HttpResponse, StatusCode}
import akka.http.scaladsl.model.HttpMessage.DiscardedEntity
import akka.http.scaladsl.unmarshalling.Unmarshaller

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

sealed trait EsProcessorResponse
case class EsSuccess(body: String) extends EsProcessorResponse
case class EsHttpFailure(statusCode: StatusCode) extends EsProcessorResponse
object EsBodyParseFailure extends EsProcessorResponse
object EsUnreachable extends EsProcessorResponse

object ElasticSearchResponseProcessor {

  sealed trait EsProcessorCommand
  case class ProcessEsResponse(
                                future: Future[HttpResponse],
                                replyTo: ActorRef[EsProcessorResponse]
                              ) extends EsProcessorCommand

  private final case class ProcessHttpResponse(
                                                httpResponse: HttpResponse,
                                                replyTo: ActorRef[EsProcessorResponse]
                                              ) extends EsProcessorCommand

  private final case class ReturnProcessorResponse(
                                                    response: EsProcessorResponse,
                                                    replyTo: ActorRef[EsProcessorResponse]
                                                  ) extends EsProcessorCommand

  def apply(): Behavior[EsProcessorCommand] = {
    Behaviors.setup { context =>
      Behaviors.receiveMessage[EsProcessorCommand] {

        case ProcessEsResponse(futureHttpResponse, replyTo) =>
          // Map the Future value to a message, handled by this actor
          context.pipeToSelf(futureHttpResponse) {
            case Success(httpResponse) =>
              ProcessHttpResponse(httpResponse, replyTo)
            case Failure(e) =>
              ReturnProcessorResponse(EsUnreachable, replyTo)
          }
          Behaviors.same

        case ProcessHttpResponse(httpResponse, replyTo) =>
          httpResponse.status.intValue match {
            case 200 =>
              val futureBody: Future[String] = getBody(context.system, httpResponse)

              // Map the Future value to a message, handled by this actor
              context.pipeToSelf(futureBody) {
                case Success(body) =>
                  ReturnProcessorResponse(EsSuccess(body), replyTo)
                case Failure(_) =>
                  ReturnProcessorResponse(EsBodyParseFailure, replyTo)
              }
              Behaviors.same

            case _ =>
              // The entity must be discarded, or the data will remain back-pressured.
              implicit val system: ActorSystem[Nothing] = context.system
              val discarded: DiscardedEntity = httpResponse.discardEntityBytes() // pipes data to a sink

              // Map the Future value to a message, handled by this actor
              context.pipeToSelf(discarded.future) {
                case Success(_) =>
                  ReturnProcessorResponse(EsHttpFailure(httpResponse.status), replyTo)
                case Failure(_) =>
                  ReturnProcessorResponse(EsBodyParseFailure, replyTo)
              }
              Behaviors.same
          }

        case ReturnProcessorResponse(response, replyTo) =>
          // Send reply to original requester
          replyTo ! response
          Behaviors.same
      }
    }
  }

  private def getBody(implicit system: ActorSystem[Nothing], httpResponse: HttpResponse): Future[String] = {
    implicit val ec: ExecutionContextExecutor = system.executionContext
    Unmarshaller.stringUnmarshaller(httpResponse.entity)
  }
}
