package dpla.ebookapi.v1.search

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.search.SearchProtocol.{FetchNotFound, IntermediateSearchResult, SearchFailure, SearchQuery, ValidFetchId}


object MockEsClientNotFound {

  def apply(): Behavior[IntermediateSearchResult] = {
    Behaviors.receiveMessage[IntermediateSearchResult] {

      case SearchQuery(_, _, replyTo) =>
        replyTo ! SearchFailure
        Behaviors.same

      case ValidFetchId(_, replyTo) =>
        replyTo ! FetchNotFound
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}
