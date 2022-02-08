package dpla.ebookapi.v1.ebooks

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.HttpMessage.DiscardedEntity
import akka.http.scaladsl.unmarshalling.Unmarshaller

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

sealed trait ElasticSearchResponse
case class ElasticSearchSuccess(body: String) extends ElasticSearchResponse
case class ElasticSearchHttpFailure(status: Int) extends ElasticSearchResponse
object ElasticSearchParseFailure extends ElasticSearchResponse
object ElasticSearchUnreachable extends ElasticSearchResponse

/**
 * Processes response streams from Elastic Search.
 */
object ElasticSearchResponseProcessor {

  sealed trait ElasticSearchResponseProcessorCommand
  case class ProcessElasticSearchResponse(
                                           future: Future[HttpResponse],
                                           replyTo: ActorRef[ElasticSearchResponse]
                                         ) extends ElasticSearchResponseProcessorCommand

  private final case class ProcessHttpResponse(
                                                httpResponse: HttpResponse,
                                                replyTo: ActorRef[ElasticSearchResponse]
                                              ) extends ElasticSearchResponseProcessorCommand

  private final case class ReturnProcessorResponse(
                                                    response: ElasticSearchResponse,
                                                    replyTo: ActorRef[ElasticSearchResponse]
                                                  ) extends ElasticSearchResponseProcessorCommand

  def apply(): Behavior[ElasticSearchResponseProcessorCommand] = {
    Behaviors.setup { context =>
      Behaviors.receiveMessage[ElasticSearchResponseProcessorCommand] {

        case ProcessElasticSearchResponse(futureHttpResponse, replyTo) =>
          // Map the Future value to a message, handled by this actor.
          context.pipeToSelf(futureHttpResponse) {
            case Success(httpResponse) =>
              ProcessHttpResponse(httpResponse, replyTo)
            case Failure(e) =>
              // TODO log error
              ReturnProcessorResponse(ElasticSearchUnreachable, replyTo)
          }
          Behaviors.same

        case ProcessHttpResponse(httpResponse, replyTo) =>
          httpResponse.status.intValue match {
            case 200 =>
              // If response status is 200, get the response body as a String.
              val futureBody: Future[String] = getEntityString(context.system, httpResponse)

              // Map the Future value to a message, handled by this actor.
              context.pipeToSelf(futureBody) {
                case Success(body) =>
                  ReturnProcessorResponse(ElasticSearchSuccess(body), replyTo)
                case Failure(e) =>
                  // TODO log error
                  ReturnProcessorResponse(ElasticSearchParseFailure, replyTo)
              }
              Behaviors.same

            case _ =>
              // If response status, is not 200, the entity must be discarded.
              // Otherwise, the data will remain back-pressured.
              implicit val system: ActorSystem[Nothing] = context.system
              val discarded: DiscardedEntity = httpResponse.discardEntityBytes() // pipes data to a sink

              // Map the Future value to a message, handled by this actor.
              context.pipeToSelf(discarded.future) {
                case Success(_) =>
                  ReturnProcessorResponse(ElasticSearchHttpFailure(httpResponse.status.intValue), replyTo)
                case Failure(e) =>
                  // TODO log error
                  ReturnProcessorResponse(ElasticSearchParseFailure, replyTo)
              }
              Behaviors.same
          }

        case ReturnProcessorResponse(response, replyTo) =>
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
