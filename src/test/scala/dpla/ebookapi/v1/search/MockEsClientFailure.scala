package dpla.ebookapi.v1.search

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.search.SearchProtocol.{IntermediateSearchResult, SearchFailure, SearchQuery, ValidFetchIds}

object MockEsClientFailure {

  def apply(): Behavior[IntermediateSearchResult] = {
    Behaviors.receiveMessage[IntermediateSearchResult] {

      case SearchQuery(_, _, replyTo) =>
        replyTo ! SearchFailure
        Behaviors.same

      case ValidFetchIds(_, replyTo) =>
        replyTo ! SearchFailure
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}
