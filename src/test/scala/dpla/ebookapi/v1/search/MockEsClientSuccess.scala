package dpla.ebookapi.v1.search

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import dpla.ebookapi.helpers.FileReader
import dpla.ebookapi.v1.search.SearchProtocol.{FetchQuery, FetchQueryResponse, IntermediateSearchResult, SearchQuery, SearchQueryResponse}


object MockEsClientSuccess extends FileReader {

  private val searchBody: String =
    readFile("/elasticSearchEbookList.json")

  private val fetchBody: String =
    readFile("/elasticSearchEbook.json")

  def apply(mapper: ActorRef[IntermediateSearchResult]): Behavior[IntermediateSearchResult] = {
    Behaviors.receiveMessage[IntermediateSearchResult] {

      case SearchQuery(params, _, replyTo) =>
        mapper ! SearchQueryResponse(params, searchBody, replyTo)
        Behaviors.same

      case FetchQuery(_, replyTo) =>
        mapper ! FetchQueryResponse(fetchBody, replyTo)
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}
