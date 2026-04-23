package dpla.api.v2.search

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import akka.actor.typed.scaladsl.adapter.TypedSchedulerOps
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  HttpMethods,
  HttpRequest,
  HttpResponse
}
import akka.pattern.CircuitBreaker
import dpla.api.v2.search.ElasticSearchResponseHandler.{
  ElasticSearchResponseHandlerCommand,
  ProcessElasticSearchResponse
}
import dpla.api.v2.search.SearchProtocol._
import dpla.api.v2.search.paramValidators.{
  FetchParams,
  RandomParams,
  SearchParams
}
import spray.json.JsValue

import java.util.concurrent.{Semaphore, TimeUnit}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.DurationConverters._
import org.slf4j.LoggerFactory

/** Sends requests to Elastic Search.
  */
object ElasticSearchClient {

  private val log = LoggerFactory.getLogger(getClass)

  // Default values for configuration
  private val DefaultMaxConcurrentRequests: Int = 32
  private val DefaultSemaphoreTimeoutSeconds: Long = 5L

  // Concurrency limiter to prevent overwhelming the ES cluster and Akka HTTP pool.
  // This caps the number of concurrent in-flight ES requests per API instance.
  private val maxConcurrentEsRequests: Int = {
    val envVar = "ES_MAX_CONCURRENT_REQUESTS"
    sys.env.get(envVar) match {
      case Some(value) =>
        value.toIntOption match {
          case Some(parsed) if parsed > 0 => parsed
          case Some(parsed)               =>
            log.error(
              s"Invalid value for $envVar: '$value' (must be positive integer). " +
                s"Using default: $DefaultMaxConcurrentRequests"
            )
            DefaultMaxConcurrentRequests
          case None =>
            log.error(
              s"Invalid value for $envVar: '$value' (not a valid integer). " +
                s"Using default: $DefaultMaxConcurrentRequests"
            )
            DefaultMaxConcurrentRequests
        }
      case None => DefaultMaxConcurrentRequests
    }
  }
  private val semaphore = new Semaphore(maxConcurrentEsRequests)

  // Timeout for acquiring a semaphore permit (seconds).
  // If exceeded, the request fails fast rather than blocking indefinitely.
  // Keep this well below askTimeout (30s) to leave time for the actual ES query.
  private val semaphoreTimeoutSeconds: Long = {
    val envVar = "ES_SEMAPHORE_TIMEOUT_SECONDS"
    sys.env.get(envVar) match {
      case Some(value) =>
        value.toLongOption match {
          case Some(parsed) if parsed > 0 => parsed
          case Some(parsed)               =>
            log.error(
              s"Invalid value for $envVar: '$value' (must be positive integer). " +
                s"Using default: $DefaultSemaphoreTimeoutSeconds"
            )
            DefaultSemaphoreTimeoutSeconds
          case None =>
            log.error(
              s"Invalid value for $envVar: '$value' (not a valid integer). " +
                s"Using default: $DefaultSemaphoreTimeoutSeconds"
            )
            DefaultSemaphoreTimeoutSeconds
        }
      case None => DefaultSemaphoreTimeoutSeconds
    }
  }

  /** Wraps an ES request Future with concurrency limiting. Uses tryAcquire with
    * timeout to avoid blocking actor threads indefinitely. Ensures permit is
    * released even if Future construction fails.
    */
  private def withConcurrencyLimit[T](
      f: => Future[T]
  )(implicit ec: ExecutionContext): Future[T] = {
    if (!semaphore.tryAcquire(semaphoreTimeoutSeconds, TimeUnit.SECONDS)) {
      Future.failed(
        new RuntimeException(
          s"ES request rejected: concurrency limit ($maxConcurrentEsRequests) exceeded, " +
            s"timed out after ${semaphoreTimeoutSeconds}s waiting for permit"
        )
      )
    } else {
      try {
        val future = f
        future.andThen { case _ => semaphore.release() }(ec)
      } catch {
        case e: Throwable =>
          semaphore.release()
          Future.failed(e)
      }
    }
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

      val cfg = context.system.settings.config.getConfig("elasticSearch.circuitBreaker")
      val maxFailures: Int = cfg.getInt("maxFailures")
      val callTimeout: FiniteDuration = cfg.getDuration("callTimeout").toScala.toCoarsest
      val resetTimeout: FiniteDuration = cfg.getDuration("resetTimeout").toScala.toCoarsest
      val breaker = CircuitBreaker(
        scheduler    = context.system.scheduler.toClassic,
        maxFailures  = maxFailures,
        callTimeout  = callTimeout,
        resetTimeout = resetTimeout
      ).onOpen(() => context.log.warn("ElasticSearch circuit breaker opened"))
       .onClose(() => context.log.info("ElasticSearch circuit breaker closed"))
       .onHalfOpen(() => context.log.info("ElasticSearch circuit breaker half-open, testing ES"))

      Behaviors.receiveMessage[IntermediateSearchResult] {

        case SearchQuery(params, query, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor = processSearch(
            params,
            query,
            endpoint,
            replyTo,
            responseHandler,
            nextPhase,
            breaker
          )
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case FetchQuery(id, params, query, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor = processFetch(
            id,
            params,
            query,
            endpoint,
            replyTo,
            responseHandler,
            nextPhase,
            breaker
          )
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case MultiFetchQuery(query, replyTo) =>
          val sessionChildActor = processMultiFetch(
            query,
            endpoint,
            replyTo,
            responseHandler,
            nextPhase,
            breaker
          )
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case RandomQuery(params, query, replyTo) =>
          // Create a session child actor to process the request.
          val sessionChildActor = processRandom(
            params,
            query,
            endpoint,
            replyTo,
            responseHandler,
            nextPhase,
            breaker
          )
          context.spawnAnonymous(sessionChildActor)
          Behaviors.same

        case _ =>
          Behaviors.unhandled
      }
    }
  }

  /** Per session actor behavior for handling a search request. The session
    * actor has its own internal state and its own ActorRef for
    * sending/receiving messages.
    */
  private def processSearch(
      params: SearchParams,
      query: JsValue,
      endpoint: String,
      replyTo: ActorRef[SearchResponse],
      responseHandler: ActorRef[ElasticSearchResponseHandlerCommand],
      nextPhase: ActorRef[IntermediateSearchResult],
      breaker: CircuitBreaker
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
        breaker.withCircuitBreaker { Http().singleRequest(request) }
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

        case ElasticSearchHttpError(statusCode) =>
          if (statusCode.intValue == 400)
            replyTo ! SearchQueryParseFailure
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

  private def processFetch(
      id: String,
      params: Option[FetchParams],
      query: Option[JsValue],
      endpoint: String,
      replyTo: ActorRef[SearchResponse],
      responseHandler: ActorRef[ElasticSearchResponseHandlerCommand],
      nextPhase: ActorRef[IntermediateSearchResult],
      breaker: CircuitBreaker
  ): Behavior[ElasticSearchResponse] = {

    Behaviors.setup { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      implicit val ec: ExecutionContext = system.executionContext

      // Make an HTTP request to elastic search.
      val fetchUri = s"$endpoint/_doc/$id"
      val futureResp: Future[HttpResponse] = withConcurrencyLimit {
        breaker.withCircuitBreaker { Http().singleRequest(HttpRequest(uri = fetchUri)) }
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

  /** Per session actor behavior for handling a multi-fetch request. The session
    * actor has its own internal state and its own ActorRef for
    * sending/receiving messages.
    */
  private def processMultiFetch(
      query: JsValue,
      endpoint: String,
      replyTo: ActorRef[SearchResponse],
      responseHandler: ActorRef[ElasticSearchResponseHandlerCommand],
      nextPhase: ActorRef[IntermediateSearchResult],
      breaker: CircuitBreaker
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
        breaker.withCircuitBreaker { Http().singleRequest(request) }
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
      nextPhase: ActorRef[IntermediateSearchResult],
      breaker: CircuitBreaker
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
        breaker.withCircuitBreaker { Http().singleRequest(request) }
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

        case ElasticSearchHttpError(statusCode) =>
          if (statusCode.intValue == 400)
            replyTo ! SearchQueryParseFailure
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
}
