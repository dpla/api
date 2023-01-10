package dpla.api.v2.search

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import dpla.api.helpers.FileReader
import dpla.api.v2.search.SearchProtocol.{FetchQuery, FetchQueryResponse, IntermediateSearchResult, MultiFetchQuery, MultiFetchQueryResponse, SearchQuery, SearchQueryResponse}


object MockEboookEsClientSuccess extends FileReader {

  private val searchBody: String =
    readFile("/elasticSearchEbookList.json")

  private val fetchBody: String =
    readFile("/elasticSearchEbook.json")

  def apply(nextPhase: ActorRef[IntermediateSearchResult]): Behavior[IntermediateSearchResult] = {
    Behaviors.receiveMessage[IntermediateSearchResult] {

      case SearchQuery(params, _, replyTo) =>
        nextPhase ! SearchQueryResponse(params, searchBody, replyTo)
        Behaviors.same

      case FetchQuery(_, params, _, replyTo) =>
        nextPhase ! FetchQueryResponse(params, fetchBody, replyTo)
        Behaviors.same

      case MultiFetchQuery(_, replyTo) =>
        nextPhase ! MultiFetchQueryResponse(searchBody, replyTo)
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}
