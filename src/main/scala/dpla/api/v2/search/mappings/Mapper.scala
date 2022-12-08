package dpla.api.v2.search.mappings

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.api.v2.search.SearchProtocol._
import dpla.api.v2.search.paramValidators.SearchParams

import scala.util.{Failure, Success, Try}

/**
 * Maps responses to case classes.
 */

trait MappedResponse

trait Mapper {

  /** Abstract methods */
  protected def mapDocList(body: String, searchParams: Option[SearchParams] = None): Try[MappedResponse]
  protected def mapSingleDoc(body: String): Try[MappedResponse]

  def apply(): Behavior[IntermediateSearchResult] = {

    Behaviors.setup[IntermediateSearchResult] { context =>

      Behaviors.receiveMessage[IntermediateSearchResult] {

        case SearchQueryResponse(params, body, replyTo) =>
          mapDocList(body, Some(params)) match {
            case Success(mappedDocList) =>
              replyTo ! MappedSearchResult(mappedDocList)
            case Failure(e) =>
              context.log.error(
                "Failed to parse MappedDocList from ElasticSearch response:", e
              )
              replyTo ! SearchFailure
          }
          Behaviors.same

        case FetchQueryResponse(params, body, replyTo) =>
          mapSingleDoc(body) match {
            case Success(singleMappedDoc) =>
              replyTo ! MappedFetchResult(singleMappedDoc)
            case Failure(e) =>
              context.log.error(
                "Failed to parse SingleMappedDoc from ElasticSearch response:", e
              )
              replyTo ! SearchFailure
          }
          Behaviors.same

        case MultiFetchQueryResponse(body, replyTo) =>
          mapDocList(body) match {
            case Success(multiDoc) =>
              replyTo ! MappedMultiFetchResult(multiDoc)
            case Failure(e) =>
              context.log.error(
                "Failed to parse MappedDocList from ElasticSearch response:", e
              )
              replyTo ! SearchFailure
          }
          Behaviors.same

        case RandomQueryResponse(_, body, replyTo) =>
          mapDocList(body) match {
            case Success(randomDoc) =>
              replyTo ! MappedRandomResult(randomDoc)
            case Failure(e) =>
              context.log.error(
                "Failed to parse MappedDocList from ElasticSearch response:", e
              )
              replyTo ! SearchFailure
          }
          Behaviors.same

        case _ =>
          Behaviors.unhandled
      }
    }
  }
}
