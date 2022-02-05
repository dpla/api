package dpla.ebookapi.v1.ebooks

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCode}

import scala.concurrent.Future


sealed trait EsClientResponse
case class ElasticSearchResponse(future: Future[HttpResponse]) extends EsClientResponse

// TODO do I need to implement retries, incremental backoff, etc. or does akka-http handle that?
object ElasticSearchClient extends ElasticSearchQueryBuilder {

  sealed trait EsClientCommand

  final case class GetEsSearchResult(
                                      params: SearchParams,
                                      replyTo: ActorRef[EsClientResponse]
                                    ) extends EsClientCommand

  final case class GetEsFetchResult(
                                     params: FetchParams,
                                     replyTo: ActorRef[EsClientResponse]
                                   ) extends EsClientCommand

  private val elasticSearchEndpoint: String = System.getenv("ELASTICSEARCH_URL") match {
    case "" => "http://localhost:9200/eleanor"
    case x => x
  }

  def apply(): Behavior[EsClientCommand] = {
    Behaviors.setup { context =>
      Behaviors.receiveMessage[EsClientCommand] {

        case GetEsSearchResult(params, replyTo) =>
          replyTo ! ElasticSearchResponse(search(context.system, params))
          Behaviors.same

        case GetEsFetchResult(params, replyTo) =>
          replyTo ! ElasticSearchResponse(fetch(context.system, params))
          Behaviors.same
      }
    }
  }

  private def search(implicit system: ActorSystem[Nothing], params: SearchParams): Future[HttpResponse] = {
    val uri: String = s"$elasticSearchEndpoint/_search"
    val data: String = composeSearchQuery(params).toString

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
}
