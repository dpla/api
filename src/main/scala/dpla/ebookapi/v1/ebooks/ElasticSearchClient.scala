package dpla.ebookapi.v1.ebooks

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import akka.util.Timeout
import dpla.ebookapi.v1.ebooks.ElasticSearchQueryBuilder.GetSearchQuery
import dpla.ebookapi.v1.ebooks.ElasticSearchResponseProcessor.ProcessElasticSearchResponse

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt


// TODO do I need to implement retries, incremental backoff, etc. or does akka-http handle that?
object ElasticSearchClient {

  sealed trait EsClientCommand

  final case class GetEsSearchResult(
                                      params: SearchParams,
                                      replyTo: ActorRef[ElasticSearchResponse]
                                    ) extends EsClientCommand

  final case class GetEsFetchResult(
                                     params: FetchParams,
                                     replyTo: ActorRef[ElasticSearchResponse]
                                   ) extends EsClientCommand

  private val elasticSearchEndpoint: String = System.getenv("ELASTICSEARCH_URL") match {
    case "" => "http://localhost:9200/eleanor"
    case x => x.stripSuffix("/")
  }
  private val searchUri: String = s"$elasticSearchEndpoint/_search"
  private def fetchUri(id: String): String = s"$elasticSearchEndpoint/_doc/$id"

  def apply(): Behavior[EsClientCommand] = {
    Behaviors.setup { context =>

      val queryBuilder: ActorRef[ElasticSearchQueryBuilder.EsQueryBuilderCommand] =
        context.spawn(ElasticSearchQueryBuilder(), "ElasticSearchQueryBuilder")
      val responseProcessor: ActorRef[ElasticSearchResponseProcessor.ElasticSearchResponseProcessorCommand] =
        context.spawn(ElasticSearchResponseProcessor(), "ElasticSearchResponseProcessor")

      // TODO move timeout
      implicit val timeout: Timeout = 3.seconds
      implicit val scheduler: Scheduler = context.system.scheduler
      implicit val system: ActorSystem[Nothing] = context.system

      Behaviors.receiveMessage[EsClientCommand] {

        case GetEsSearchResult(params, replyTo) =>
          // Create a session child actor to process the request
          val sessionChildActor =
            processSearch(params, replyTo, queryBuilder, responseProcessor)
          val uniqueId = java.util.UUID.randomUUID.toString
          context.spawn(sessionChildActor, s"ProcessEsSearchRequest-$uniqueId")
          Behaviors.same

        case GetEsFetchResult(params, replyTo) =>
          // make an HTTP request to elastic search
          val id = params.id
          val uri = fetchUri(id)
          val futureResponse: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = uri))
          // send the response future be processed
          responseProcessor ! ProcessElasticSearchResponse(futureResponse, replyTo)
          Behaviors.same
      }
    }
  }

  private def processSearch(
                             params: SearchParams,
                             replyTo: ActorRef[ElasticSearchResponse],
                             queryBuilder: ActorRef[ElasticSearchQueryBuilder.EsQueryBuilderCommand],
                             responseProcessor: ActorRef[ElasticSearchResponseProcessor.ElasticSearchResponseProcessorCommand]
                           ): Behavior[EsQueryBuilderResponse] = {

    Behaviors.setup[EsQueryBuilderResponse] { context =>

      implicit val system: ActorSystem[Nothing] = context.system
      // get the search query
      queryBuilder ! GetSearchQuery(params, context.self)

      Behaviors.receiveMessage[EsQueryBuilderResponse] {
        case ElasticSearchQuery(query) =>
          // upon receiving the search query, make an http request to elastic search
          val request: HttpRequest = HttpRequest(
            method = HttpMethods.GET,
            uri = searchUri,
            entity = HttpEntity(ContentTypes.`application/json`, query.toString)
          )
          val futureResponse: Future[HttpResponse] = Http().singleRequest(request)
          // send the response future be processed
          responseProcessor ! ProcessElasticSearchResponse(futureResponse, replyTo)
          Behaviors.stopped

        case _ =>
          // TODO log
          Behaviors.unhandled
      }
    }
  }
}
