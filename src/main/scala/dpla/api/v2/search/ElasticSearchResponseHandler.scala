package dpla.api.v2.search

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.{HttpResponse, StatusCode}
import akka.http.scaladsl.model.HttpMessage.DiscardedEntity
import akka.http.scaladsl.unmarshalling.Unmarshaller

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

/**
 * Processes response streams from Elastic Search.
 */

sealed trait ElasticSearchResponse

final case class ElasticSearchSuccess(
                                       body: String
                                     ) extends ElasticSearchResponse

final case class ElasticSearchHttpError(
                                         statusCode: StatusCode
                                       ) extends ElasticSearchResponse

case object ElasticSearchResponseFailure extends ElasticSearchResponse


object ElasticSearchResponseHandler {

  sealed trait ElasticSearchResponseHandlerCommand

  final case class ProcessElasticSearchResponse(
                                                 future: Future[HttpResponse],
                                                 replyTo: ActorRef[ElasticSearchResponse]
                                               ) extends ElasticSearchResponseHandlerCommand

  private final case class ProcessHttpResponse(
                                                httpResponse: HttpResponse,
                                                replyTo: ActorRef[ElasticSearchResponse]
                                              ) extends ElasticSearchResponseHandlerCommand

  private final case class ReturnFinalResponse(
                                                response: ElasticSearchResponse,
                                                replyTo: ActorRef[ElasticSearchResponse],
                                                error: Option[Throwable] = None
                                              ) extends ElasticSearchResponseHandlerCommand

  def apply(): Behavior[ElasticSearchResponseHandlerCommand] = {
    Behaviors.setup { context =>
      Behaviors.receiveMessage[ElasticSearchResponseHandlerCommand] {

        case ProcessElasticSearchResponse(futureHttpResponse, replyTo) =>
          // Map the Future value to a message, handled by this actor.
          context.pipeToSelf(futureHttpResponse) {
            case Success(httpResponse) =>
              ProcessHttpResponse(httpResponse, replyTo)
            case Failure(e) =>
              // Failed to reach ElasticSearch
              ReturnFinalResponse(ElasticSearchResponseFailure, replyTo, Some(e))
          }
          Behaviors.same

        case ProcessHttpResponse(httpResponse, replyTo) =>
          if (httpResponse.status.isSuccess) {
            // If response status is 200, get the response body as a String.
            val futureBody: Future[String] =
              getEntityString(context.system, httpResponse)

            // Map the Future value to a message, handled by this actor.
            context.pipeToSelf(futureBody) {
              case Success(body) =>
                ReturnFinalResponse(ElasticSearchSuccess(body), replyTo)
              case Failure(e) =>
                ReturnFinalResponse(ElasticSearchResponseFailure, replyTo, Some(e))
            }
            Behaviors.same

          } else {
            // If response status, is not 200, the entity must be discarded.
            // Otherwise, the data will remain back-pressured.
            // discardEntityBytes() pipes data to a sink.
            implicit val system: ActorSystem[Nothing] = context.system
            val discarded: DiscardedEntity = httpResponse.discardEntityBytes()

            // Map the Future value to a message, handled by this actor.
            context.pipeToSelf(discarded.future) {
              case Success(_) =>
                ReturnFinalResponse(
                  ElasticSearchHttpError(httpResponse.status), replyTo
                )
              case Failure(e) =>
                ReturnFinalResponse(ElasticSearchResponseFailure, replyTo, Some(e))
            }
            Behaviors.same
        }

        case ReturnFinalResponse(response, replyTo, error) =>
          // Log error if there is one
          error match {
            case Some(e) =>
              context.log.error(
                "Failed to process ElasticSearch response:", e
              )
            case None => // no-op
          }
          // Send fully processed reply to original requester.
          replyTo ! response
          Behaviors.same
      }
    }
  }

  private def getEntityString(implicit system: ActorSystem[Nothing],
                              httpResponse: HttpResponse): Future[String] = {

    implicit val ec: ExecutionContextExecutor = system.executionContext
    Unmarshaller.stringUnmarshaller(httpResponse.entity)
  }
}
