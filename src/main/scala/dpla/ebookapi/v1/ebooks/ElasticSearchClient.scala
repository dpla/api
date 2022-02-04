package dpla.ebookapi.v1.ebooks

import akka.Done
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCode}
import akka.http.scaladsl.model.HttpMessage.DiscardedEntity
import akka.http.scaladsl.unmarshalling.Unmarshaller


import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}


sealed trait EsClientResponse
// TODO make separate case classes for EsSearchResult and EsFetchResult?
case class EsSuccess(body: String) extends EsClientResponse
case class EsHttpFailure(statusCode: StatusCode) extends EsClientResponse
case class EsBodyParseFailure(message: String) extends EsClientResponse
case class EsUnreachable(message: String) extends EsClientResponse

// TODO do I need to implement retries, incremental backoff, etc. or does akka-http handle that?
object ElasticSearchClient extends ElasticSearchQueryBuilder {

  sealed trait EsClientCommand

  final case class GetSearchResult(
                                    params: SearchParams,
                                    replyTo: ActorRef[EsClientResponse]
                                  ) extends EsClientCommand

  final case class GetFetchResult(
                                   params: FetchParams,
                                   replyTo: ActorRef[EsClientResponse]
                                 ) extends EsClientCommand

  private final case class ProcessHttpResponse(
                                                httpResponse: HttpResponse,
                                                replyTo: ActorRef[EsClientResponse]
                                              ) extends EsClientCommand

  private final case class ReturnEsClientResponse(
                                                   response: EsClientResponse,
                                                  replyTo: ActorRef[EsClientResponse]
                                                 ) extends EsClientCommand

  val elasticSearchEndpoint: String = System.getenv("ELASTICSEARCH_URL") match {
    case "" => "http://localhost:9200/eleanor"
    case x => x
  }

  // TODO implement session actor - is this NECESSARY?  Not if replyTo is maintained?  Maybe a good idea anyway?
  def apply(): Behavior[EsClientCommand] =
    Behaviors.receive { (context, command) =>
      command match {
        case GetSearchResult(params, replyTo) =>
          val futureHttpResponse: Future[HttpResponse] = search(context.system, params)

          // Map the Future value to a message, handled by this actor
          context.pipeToSelf(futureHttpResponse) {
            case Success(httpResponse) =>
              ProcessHttpResponse(httpResponse, replyTo)
            case Failure(e) =>
              ReturnEsClientResponse(EsUnreachable(e.getMessage), replyTo)
          }

          Behaviors.same

        case GetFetchResult(params, replyTo) =>
          val futureHttpResponse: Future[HttpResponse] = fetch(context.system, params)

          // Map the Future value to a message, handled by this actor
          context.pipeToSelf(futureHttpResponse) {
            case Success(httpResponse) =>
              ProcessHttpResponse(httpResponse, replyTo)
            case Failure(e) =>
              ReturnEsClientResponse(EsUnreachable(e.getMessage), replyTo)
          }

          Behaviors.same

        case ProcessHttpResponse(httpResponse, replyTo) =>
          httpResponse.status.intValue match {
            case 200 =>
              val futureBody: Future[String] = getBody(context.system, httpResponse)

              // Map the Future value to a message, handled by this actor
              context.pipeToSelf(futureBody) {
                case Success(body) =>
                  ReturnEsClientResponse(EsSuccess(body), replyTo)
                case Failure(e) =>
                  ReturnEsClientResponse(EsBodyParseFailure(e.getMessage), replyTo)
              }

              Behaviors.same

            case _ =>
              // The entity must be discarded, or the data will remain back-pressured.
              implicit val system: ActorSystem[Nothing] = context.system
              val discarded: DiscardedEntity = httpResponse.discardEntityBytes() // pipes data to a sink

              // Map the Future value to a message, handled by this actor
              context.pipeToSelf(discarded.future) {
                case Success(_) =>
                  ReturnEsClientResponse(EsHttpFailure(httpResponse.status), replyTo)
                case Failure(e) =>
                  ReturnEsClientResponse(EsBodyParseFailure(e.getMessage), replyTo)
              }

              Behaviors.same
          }

        case ReturnEsClientResponse(response, replyTo) =>
          // Send reply to original requester
          replyTo ! response
          Behaviors.same
      }
    }

  private def search(implicit system: ActorSystem[Nothing], params: SearchParams): Future[HttpResponse] = {
    val uri: String = s"$elasticSearchEndpoint/_search"
    val data: String = composeQuery(params).toString

    val request: HttpRequest = HttpRequest(
      method = HttpMethods.GET,
      uri = uri,
      entity = HttpEntity(ContentTypes.`application/json`, data)
    )

    Http().singleRequest(request)
  }

  private def fetch(implicit system: ActorSystem[Nothing], params: FetchParams): Future[HttpResponse] = {
    val id = params.id
    val uri = s"$elasticSearchEndpoint/_doc/$id"
    Http().singleRequest(HttpRequest(uri = uri))
  }

  private def getBody(implicit system: ActorSystem[Nothing], httpResponse: HttpResponse): Future[String] = {
    implicit val ec: ExecutionContextExecutor = system.executionContext
    Unmarshaller.stringUnmarshaller(httpResponse.entity)
  }
}
