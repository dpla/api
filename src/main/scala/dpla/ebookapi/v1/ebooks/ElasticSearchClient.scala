package dpla.ebookapi.v1.ebooks

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import dpla.ebookapi.v1.ebooks.ElasticSearchQueryBuilder.{EsQueryBuilderCommand, GetSearchQuery}
import dpla.ebookapi.v1.ebooks.ElasticSearchResponseHandler.{ElasticSearchResponseHandlerCommand, ProcessElasticSearchResponse}

import scala.concurrent.Future

/**
 * Composes and sends requests to Elastic Search and processes response streams.
 * Its children include ElasticSearchQueryBuilder, ElasticSearchResponseHandler,
 * and session actors.
 * It also messages with EbookRegistry.
 */
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

  def apply(endpoint: String): Behavior[EsClientCommand] = {
    Behaviors.setup { context =>

      // Spawn children.
      val queryBuilder: ActorRef[EsQueryBuilderCommand] =
        context.spawn(
          ElasticSearchQueryBuilder(),
          "ElasticSearchQueryBuilder"
        )

      val responseHandler: ActorRef[ElasticSearchResponseHandlerCommand] =
        context.spawn(
          ElasticSearchResponseHandler(),
          "ElasticSearchResponseProcessor"
        )

      Behaviors.receiveMessage[EsClientCommand] {

        case GetEsSearchResult(params, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor =
            processSearch(params, endpoint, replyTo, queryBuilder, responseHandler)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case GetEsFetchResult(params, replyTo) =>
          // Make an HTTP request to elastic search.
          val id = params.id
          val fetchUri = s"$endpoint/_doc/$id"
          implicit val system: ActorSystem[Nothing] = context.system
          val futureResp: Future[HttpResponse] =
            Http().singleRequest(HttpRequest(uri = fetchUri))

          context.log.info(s"ElasticSearch fetch QUERY: {}", fetchUri)

          // Send the response future be processed.
          // Tell ElasticSearchResponseProcessor to reply directly to
          // EbookRegistry.
          responseHandler ! ProcessElasticSearchResponse(futureResp, replyTo)
          Behaviors.same
      }
    }
  }

  /**
   * Per session actor behavior for handling a search request.
   * The session actor has its own internal state and its own ActorRef for
   * sending/receiving messages.
   */
  private def processSearch(
                             params: SearchParams,
                             endpoint: String,
                             replyTo: ActorRef[ElasticSearchResponse],
                             queryBuilder: ActorRef[EsQueryBuilderCommand],
                             responseProcessor: ActorRef[ElasticSearchResponseHandlerCommand]
                           ): Behavior[EsQueryBuilderResponse] = {

    Behaviors.setup[EsQueryBuilderResponse] { context =>

      implicit val system: ActorSystem[Nothing] = context.system
      lazy val searchUri: String = s"$endpoint/_search"

      // Send initial message to ElasticSearchQueryBuilder
      queryBuilder ! GetSearchQuery(params, context.self)

      Behaviors.receiveMessage[EsQueryBuilderResponse] {
        case ElasticSearchQuery(query) =>
          // Upon receiving the search query, make HTTP request to ElasticSearch
          val request: HttpRequest = HttpRequest(
            method = HttpMethods.GET,
            uri = searchUri,
            entity = HttpEntity(ContentTypes.`application/json`, query.toString)
          )
          val futureResp: Future[HttpResponse] =
            Http().singleRequest(request)

          context.log.info2(
            "ElasticSearch search QUERY: {}: {}",
            searchUri,
            query.toString
          )

          // Send the response future be processed.
          // Tell ElasticSearchResponseHandler to reply directly to
          // EbookRegistry.
          responseProcessor ! ProcessElasticSearchResponse(futureResp, replyTo)
          Behaviors.stopped

        case _ =>
          Behaviors.unhandled
      }
    }
  }
}
