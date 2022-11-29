package dpla.api.v2.search

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import dpla.api.helpers.FileReader
import dpla.api.v2.search.SearchProtocol.{FetchQuery, FetchQueryResponse, IntermediateSearchResult, MultiFetchQuery, MultiFetchQueryResponse, RandomQuery, RandomQueryResponse, SearchQuery, SearchQueryResponse}


object MockItemEsClientSuccess extends FileReader {

  private val searchBody: String =
    readFile("/elasticSearchItemList.json")

  private val fetchBody: String =
    readFile("/elasticSearchItem.json")

  def apply(nextPhase: ActorRef[IntermediateSearchResult]): Behavior[IntermediateSearchResult] = {
    Behaviors.receiveMessage[IntermediateSearchResult] {

      case SearchQuery(params, _, replyTo) =>
        nextPhase ! SearchQueryResponse(params, searchBody, replyTo)
        Behaviors.same

      case FetchQuery(_, _, _, replyTo) =>
        nextPhase ! FetchQueryResponse(None, fetchBody, replyTo)
        Behaviors.same

      case MultiFetchQuery(_, replyTo) =>
        nextPhase ! MultiFetchQueryResponse(searchBody, replyTo)
        Behaviors.same

      case RandomQuery(params, _, replyTo) =>
        nextPhase ! RandomQueryResponse(params, searchBody, replyTo)
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}
