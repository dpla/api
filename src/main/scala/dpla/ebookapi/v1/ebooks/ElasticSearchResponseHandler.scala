package dpla.ebookapi.v1.ebooks

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.{HttpResponse, StatusCode}
import akka.http.scaladsl.model.HttpMessage.DiscardedEntity
import akka.http.scaladsl.unmarshalling.Unmarshaller

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

sealed trait ElasticSearchResponse
case class ElasticSearchSuccess(body: String) extends ElasticSearchResponse
case class ElasticSearchHttpFailure(statusCode: StatusCode) extends ElasticSearchResponse
object ElasticSearchParseFailure extends ElasticSearchResponse
object ElasticSearchUnreachable extends ElasticSearchResponse

/**
 * Processes response streams from Elastic Search.
 */
object ElasticSearchResponseHandler {

  sealed trait ElasticSearchResponseHandlerCommand
  case class ProcessElasticSearchResponse(
                                           future: Future[HttpResponse],
                                           replyTo: ActorRef[ElasticSearchResponse]
                                         ) extends ElasticSearchResponseHandlerCommand

  private final case class ProcessHttpResponse(
                                                httpResponse: HttpResponse,
                                                replyTo: ActorRef[ElasticSearchResponse]
                                              ) extends ElasticSearchResponseHandlerCommand

  private final case class ReturnFinalResponse(
                                                response: ElasticSearchResponse,
                                                replyTo: ActorRef[ElasticSearchResponse]
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
              // TODO is there a better way to log stack trace?
              context.log.error(
                "Failed to reach ElasticSearch: {}",
                e.getStackTrace.mkString(System.lineSeparator + "    ")
              )
              ReturnFinalResponse(ElasticSearchUnreachable, replyTo)
          }
          Behaviors.same

        case ProcessHttpResponse(httpResponse, replyTo) =>
          if (httpResponse.status.isSuccess) {
            // If response status is 200, get the response body as a String.
            val futureBody: Future[String] = getEntityString(context.system, httpResponse)

            // Map the Future value to a message, handled by this actor.
            context.pipeToSelf(futureBody) {
              case Success(body) =>
                ReturnFinalResponse(ElasticSearchSuccess(body), replyTo)
              case Failure(e) =>
                // TODO is there a better way to log stack trace?
                context.log.error(
                  "Failed to parse ElasticSearch response String: {}",
                  e.getStackTrace.mkString(System.lineSeparator + "    ")
                )
                ReturnFinalResponse(ElasticSearchParseFailure, replyTo)
            }
            Behaviors.same

          } else {
            // If response status, is not 200, the entity must be discarded.
            // Otherwise, the data will remain back-pressured.
            implicit val system: ActorSystem[Nothing] = context.system
            val discarded: DiscardedEntity = httpResponse.discardEntityBytes() // pipes data to a sink

            // Map the Future value to a message, handled by this actor.
            context.pipeToSelf(discarded.future) {
              case Success(_) =>
                ReturnFinalResponse(ElasticSearchHttpFailure(httpResponse.status), replyTo)
              case Failure(e) =>
                // TODO is there a better way to log stack trace?
                context.log.error(
                  "Failed to discard ElasticSearch response entity: {}",
                  e.getStackTrace.mkString(System.lineSeparator + "    ")
                )
                ReturnFinalResponse(ElasticSearchParseFailure, replyTo)
            }
            Behaviors.same
        }

        case ReturnFinalResponse(response, replyTo) =>
          // Send fully processed reply to original requester.
          replyTo ! response
          Behaviors.same
      }
    }
  }

  private def getEntityString(implicit system: ActorSystem[Nothing], httpResponse: HttpResponse): Future[String] = {
    implicit val ec: ExecutionContextExecutor = system.executionContext
    Unmarshaller.stringUnmarshaller(httpResponse.entity)
  }
}
