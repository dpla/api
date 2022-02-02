package dpla.ebookapi.v1.ebooks

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import scala.concurrent.Future
import scala.util.{Failure, Success}

sealed trait EsResponse
case class EsSuccess(response: HttpResponse) extends EsResponse
case class EsFailure(message: String) extends EsResponse

object ElasticSearchClientActor extends ElasticSearchQueryBuilder {

  sealed trait EsCommand

  final case class EsSearch(
                             params: SearchParams,
                             replyTo: ActorRef[EsResponse]
                           ) extends EsCommand

  final case class EsFetch(
                            id: String,
                           replyTo: ActorRef[EsResponse]
                          ) extends EsCommand

  private final case class WrappedResponse(
                                            response: EsResponse,
                                            replyTo: ActorRef[EsResponse]
                                          ) extends EsCommand

  val elasticSearchEndpoint: String = System.getenv("ELASTICSEARCH_URL") match {
    case "" => "http://localhost:9200/eleanor"
    case x => x
  }

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")

  def apply(): Behavior[EsCommand] =
    Behaviors.receive { (context, command) =>
      command match {
        case EsSearch(params, replyTo) =>
          val futureResponse: Future[HttpResponse] = search(params)

          // Map the Future value to a message, handled by this actor
          context.pipeToSelf(futureResponse) {
            case Success(response) =>
              WrappedResponse(EsSuccess(response), replyTo)
            case Failure(e) =>
              WrappedResponse(EsFailure(e.getMessage), replyTo)
          }

          Behaviors.same

        case EsFetch(id, replyTo) =>
          val futureResponse: Future[HttpResponse] = fetch(id)

          // Map the Future value to a message, handled by this actor
          context.pipeToSelf(futureResponse) {
            case Success(response) =>
              WrappedResponse(EsSuccess(response), replyTo)
            case Failure(e) =>
              WrappedResponse(EsFailure(e.getMessage), replyTo)
          }

          Behaviors.same

        case WrappedResponse(response, replyTo) =>
          // send reply to original requester
          replyTo ! response
          Behaviors.same
      }
    }

  private def search(params: SearchParams): Future[HttpResponse] = {
    val uri: String = s"$elasticSearchEndpoint/_search"
    val data: String = composeQuery(params).toString

    val request: HttpRequest = HttpRequest(
      method = HttpMethods.GET,
      uri = uri,
      entity = HttpEntity(ContentTypes.`application/json`, data)
    )

    Http().singleRequest(request)
  }

  private def fetch(id: String): Future[HttpResponse] = {
    val uri = s"$elasticSearchEndpoint/_doc/$id"
    Http().singleRequest(HttpRequest(uri = uri))
  }
}
