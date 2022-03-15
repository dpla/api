package dpla.ebookapi.v1.search

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import dpla.ebookapi.v1.search.ElasticSearchResponseHandler.{ElasticSearchResponseHandlerCommand, ProcessElasticSearchResponse}
import dpla.ebookapi.v1.search.SearchProtocol.{FetchNotFound, FetchQuery, FetchQueryResponse, IntermediateSearchResult, MultiFetchQuery, MultiFetchQueryResponse, SearchFailure, SearchQuery, SearchQueryResponse, SearchResponse, ValidFetchIds}
import spray.json.JsValue

import scala.concurrent.Future

/**
 * Sends requests to Elastic Search.
 */
object ElasticSearchClient {

  def apply(
             endpoint: String,
             nextPhase: ActorRef[IntermediateSearchResult]
           ): Behavior[IntermediateSearchResult] = {

    Behaviors.setup { context =>

      val responseHandler: ActorRef[ElasticSearchResponseHandlerCommand] =
        context.spawn(
          ElasticSearchResponseHandler(),
          "ElasticSearchResponseProcessor"
        )

      Behaviors.receiveMessage[IntermediateSearchResult] {

        case SearchQuery(params, query, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor = processSearch(params, query, endpoint,
            replyTo, responseHandler, nextPhase)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case FetchQuery(id, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor = processFetch(id, endpoint, replyTo,
            responseHandler, nextPhase)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case MultiFetchQuery(query, replyTo) =>
          val sessionChildActor = processMultiFetch(query, endpoint, replyTo,
            responseHandler, nextPhase)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case _ =>
          Behaviors.unhandled
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
                             query: JsValue,
                             endpoint: String,
                             replyTo: ActorRef[SearchResponse],
                             responseProcessor: ActorRef[ElasticSearchResponseHandlerCommand],
                             nextPhase: ActorRef[IntermediateSearchResult]
                           ): Behavior[ElasticSearchResponse] = {

    Behaviors.setup { context =>

      implicit val system: ActorSystem[Nothing] = context.system

      // Make an HTTP request to elastic search.
      val searchUri: String = s"$endpoint/_search"
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

      // Send response future to ElasticSearchResponseProcessor
      responseProcessor ! ProcessElasticSearchResponse(futureResp, context.self)

      Behaviors.receiveMessage[ElasticSearchResponse] {

        case ElasticSearchSuccess(body) =>
          nextPhase ! SearchQueryResponse(params, body, replyTo)
          Behaviors.stopped

        case ElasticSearchHttpError(_) =>
          replyTo ! SearchFailure
          Behaviors.stopped

        case ElasticSearchResponseFailure =>
          replyTo ! SearchFailure
          Behaviors.stopped

        case _ =>
          Behaviors.unhandled
      }
    }
  }

  private def processFetch(
                            id: String,
                            endpoint: String,
                            replyTo: ActorRef[SearchResponse],
                            responseProcessor: ActorRef[ElasticSearchResponseHandlerCommand],
                            nextPhase: ActorRef[IntermediateSearchResult]
                          ): Behavior[ElasticSearchResponse] = {

    Behaviors.setup { context =>

      implicit val system: ActorSystem[Nothing] = context.system

      // Make an HTTP request to elastic search.
      val fetchUri = s"$endpoint/_doc/$id"
      val futureResp: Future[HttpResponse] =
        Http().singleRequest(HttpRequest(uri = fetchUri))

      context.log.info("ElasticSearch fetch QUERY: {}", fetchUri)

      // Send response future to ElasticSearchResponseProcessor
      responseProcessor ! ProcessElasticSearchResponse(futureResp, context.self)

      Behaviors.receiveMessage[ElasticSearchResponse] {

        case ElasticSearchSuccess(body) =>
          nextPhase ! FetchQueryResponse(body, replyTo)
          Behaviors.stopped

        case ElasticSearchHttpError(statusCode) =>
          if (statusCode.intValue == 404)
            replyTo ! FetchNotFound
          else
            replyTo ! SearchFailure
          Behaviors.stopped

        case ElasticSearchResponseFailure =>
          replyTo ! SearchFailure
          Behaviors.stopped

        case _ =>
          Behaviors.unhandled
      }
    }
  }

  /**
   * Per session actor behavior for handling a multi-fetch request.
   * The session actor has its own internal state and its own ActorRef for
   * sending/receiving messages.
   */
  private def processMultiFetch(
                                 query: JsValue,
                                 endpoint: String,
                                 replyTo: ActorRef[SearchResponse],
                                 responseProcessor: ActorRef[ElasticSearchResponseHandlerCommand],
                                 nextPhase: ActorRef[IntermediateSearchResult]
                               ): Behavior[ElasticSearchResponse] = {

    Behaviors.setup { context =>

      implicit val system: ActorSystem[Nothing] = context.system

      // Make an HTTP request to elastic search.
      val searchUri: String = s"$endpoint/_search"
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

      // Send response future to ElasticSearchResponseProcessor
      responseProcessor ! ProcessElasticSearchResponse(futureResp, context.self)

      Behaviors.receiveMessage[ElasticSearchResponse] {

        case ElasticSearchSuccess(body) =>
          nextPhase ! MultiFetchQueryResponse(body, replyTo)
          Behaviors.stopped

        case ElasticSearchHttpError(_) =>
          replyTo ! SearchFailure
          Behaviors.stopped

        case ElasticSearchResponseFailure =>
          replyTo ! SearchFailure
          Behaviors.stopped

        case _ =>
          Behaviors.unhandled
      }
    }
  }
}
