package dpla.api.v2.search

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import dpla.api.v2.search.ElasticSearchResponseHandler.{ElasticSearchResponseHandlerCommand, ProcessElasticSearchResponse}
import dpla.api.v2.search.SearchProtocol._
import dpla.api.v2.search.paramValidators.{FetchParams, RandomParams, SearchParams}
import spray.json.JsValue

import java.util.concurrent.Semaphore
import scala.concurrent.{ExecutionContext, Future}

/**
 * Sends requests to Elastic Search.
 */
object ElasticSearchClient {

  // Concurrency limiter to prevent overwhelming the ES cluster and Akka HTTP pool.
  // This caps the number of concurrent in-flight ES requests per API instance.
  // Default 32 permits; can be tuned via environment variable.
  private val maxConcurrentEsRequests: Int =
    sys.env.getOrElse("ES_MAX_CONCURRENT_REQUESTS", "32").toInt
  private val semaphore = new Semaphore(maxConcurrentEsRequests)

  /**
   * Wraps an ES request Future with concurrency limiting.
   * Acquires a permit before making the request and releases it when complete.
   */
  private def withConcurrencyLimit[T](f: => Future[T])
                                     (implicit ec: ExecutionContext): Future[T] = {
    semaphore.acquire()
    f.andThen { case _ => semaphore.release() }(ec)
  }

  def apply(
             endpoint: String,
             nextPhase: ActorRef[IntermediateSearchResult]
           ): Behavior[IntermediateSearchResult] = {

    Behaviors.setup { context =>

      val responseHandler: ActorRef[ElasticSearchResponseHandlerCommand] =
        context.spawn(
          ElasticSearchResponseHandler(),
          "ElasticSearchResponseHandler"
        )

      Behaviors.receiveMessage[IntermediateSearchResult] {

        case SearchQuery(params, query, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor = processSearch(params, query, endpoint,
            replyTo, responseHandler, nextPhase)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case FetchQuery(id, params, query, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor = processFetch(id, params, query, endpoint,
            replyTo, responseHandler, nextPhase)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case MultiFetchQuery(query, replyTo) =>
          val sessionChildActor = processMultiFetch(query, endpoint, replyTo,
            responseHandler, nextPhase)
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case RandomQuery(params, query, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor = processRandom(params, query, endpoint,
            replyTo, responseHandler, nextPhase)
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
                             responseHandler: ActorRef[ElasticSearchResponseHandlerCommand],
                             nextPhase: ActorRef[IntermediateSearchResult]
                           ): Behavior[ElasticSearchResponse] = {

    Behaviors.setup { context =>

      implicit val system: ActorSystem[Nothing] = context.system
      implicit val ec: ExecutionContext = system.executionContext

      // Make an HTTP request to elastic search.
      val searchUri: String = s"$endpoint/_search"
      val request: HttpRequest = HttpRequest(
        method = HttpMethods.GET,
        uri = searchUri,
        entity = HttpEntity(ContentTypes.`application/json`, query.toString)
      )
      val futureResp: Future[HttpResponse] = withConcurrencyLimit {
        Http().singleRequest(request)
      }

      context.log.info2(
        "ElasticSearch search QUERY: {}: {}",
        searchUri,
        query.toString
      )

      // Send response future to ElasticSearchResponseHandler
      responseHandler ! ProcessElasticSearchResponse(futureResp, context.self)

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
                            params: Option[FetchParams],
                            query: Option[JsValue],
                            endpoint: String,
                            replyTo: ActorRef[SearchResponse],
                            responseHandler: ActorRef[ElasticSearchResponseHandlerCommand],
                            nextPhase: ActorRef[IntermediateSearchResult]
                          ): Behavior[ElasticSearchResponse] = {

    Behaviors.setup { context =>

      implicit val system: ActorSystem[Nothing] = context.system
      implicit val ec: ExecutionContext = system.executionContext

      // Make an HTTP request to elastic search.
      val fetchUri = s"$endpoint/_doc/$id"
      val futureResp: Future[HttpResponse] = withConcurrencyLimit {
        Http().singleRequest(HttpRequest(uri = fetchUri))
      }

      context.log.info("ElasticSearch fetch QUERY: {}", fetchUri)

      // Send response future to ElasticSearchResponseHandler
      responseHandler ! ProcessElasticSearchResponse(futureResp, context.self)

      Behaviors.receiveMessage[ElasticSearchResponse] {

        case ElasticSearchSuccess(body) =>
          nextPhase ! FetchQueryResponse(params, body, replyTo)
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
                                 responseHandler: ActorRef[ElasticSearchResponseHandlerCommand],
                                 nextPhase: ActorRef[IntermediateSearchResult]
                               ): Behavior[ElasticSearchResponse] = {

    Behaviors.setup { context =>

      implicit val system: ActorSystem[Nothing] = context.system
      implicit val ec: ExecutionContext = system.executionContext

      // Make an HTTP request to elastic search.
      val searchUri: String = s"$endpoint/_search"
      val request: HttpRequest = HttpRequest(
        method = HttpMethods.GET,
        uri = searchUri,
        entity = HttpEntity(ContentTypes.`application/json`, query.toString)
      )
      val futureResp: Future[HttpResponse] = withConcurrencyLimit {
        Http().singleRequest(request)
      }

      context.log.info2(
        "ElasticSearch multi-fetch QUERY: {}: {}",
        searchUri,
        query.toString
      )

      // Send response future to ElasticSearchResponseHandler
      responseHandler ! ProcessElasticSearchResponse(futureResp, context.self)

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

  private def processRandom(
                             params: RandomParams,
                             query: JsValue,
                             endpoint: String,
                             replyTo: ActorRef[SearchResponse],
                             responseHandler: ActorRef[ElasticSearchResponseHandlerCommand],
                             nextPhase: ActorRef[IntermediateSearchResult]
                          ): Behavior[ElasticSearchResponse] = {

    Behaviors.setup { context =>

      implicit val system: ActorSystem[Nothing] = context.system
      implicit val ec: ExecutionContext = system.executionContext

      // Make an HTTP request to elastic search.
      val searchUri: String = s"$endpoint/_search"
      val request: HttpRequest = HttpRequest(
        method = HttpMethods.GET,
        uri = searchUri,
        entity = HttpEntity(ContentTypes.`application/json`, query.toString)
      )
      val futureResp: Future[HttpResponse] = withConcurrencyLimit {
        Http().singleRequest(request)
      }

      context.log.info2(
        "ElasticSearch random QUERY: {}: {}",
        searchUri,
        query.toString
      )

      // Send response future to ElasticSearchResponseHandler
      responseHandler ! ProcessElasticSearchResponse(futureResp, context.self)

      Behaviors.receiveMessage[ElasticSearchResponse] {

        case ElasticSearchSuccess(body) =>
          nextPhase ! RandomQueryResponse(params, body, replyTo)
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
