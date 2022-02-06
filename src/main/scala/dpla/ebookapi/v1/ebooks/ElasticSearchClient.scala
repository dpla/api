package dpla.ebookapi.v1.ebooks

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import dpla.ebookapi.v1.ebooks.ElasticSearchQueryBuilder.GetSearchQuery
import dpla.ebookapi.v1.ebooks.ElasticSearchResponseProcessor.ProcessElasticSearchResponse

import scala.concurrent.Future

/**
 * Composes and sends requests to Elastic Search and processes response streams.
 * It's children include ElasticSearchQueryBuilder, ElasticSearchResponseProcessor, and session actors.
 * It also messages with EbookRegistry.
 */
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

      Behaviors.receiveMessage[EsClientCommand] {

        case GetEsSearchResult(params, replyTo) =>
          // Create a session child actor to process the request
          val sessionChildActor =
            processSearch(params, replyTo, queryBuilder, responseProcessor)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case GetEsFetchResult(params, replyTo) =>
          // Make an HTTP request to elastic search
          val id = params.id
          val uri = fetchUri(id)
          implicit val system: ActorSystem[Nothing] = context.system
          val futureResponse: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = uri))
          // Send the response future be processed.
          // Tell ElasticSearchResponseProcessor to reply directly to EbookRegistry.
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

      // Send initial message to ElasticSearchQueryBuilder
      queryBuilder ! GetSearchQuery(params, context.self)

      Behaviors.receiveMessage[EsQueryBuilderResponse] {
        case ElasticSearchQuery(query) =>
          // Upon receiving the search query, make an http request to elastic search
          val request: HttpRequest = HttpRequest(
            method = HttpMethods.GET,
            uri = searchUri,
            entity = HttpEntity(ContentTypes.`application/json`, query.toString)
          )
          val futureResponse: Future[HttpResponse] = Http().singleRequest(request)
          // Send the response future be processed.
          // Tell ElasticSearchResponseProcessor to reply directly to EbookRegistry.
          responseProcessor ! ProcessElasticSearchResponse(futureResponse, replyTo)
          Behaviors.stopped

        case _ =>
          // TODO log
          Behaviors.unhandled
      }
    }
  }
}
