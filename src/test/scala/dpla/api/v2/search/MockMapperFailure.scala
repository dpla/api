package dpla.api.v2.search

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.api.v2.search.SearchProtocol.{FetchQueryResponse, IntermediateSearchResult, SearchFailure, SearchQueryResponse}

object MockMapperFailure {

  def apply(): Behavior[IntermediateSearchResult] = {
    Behaviors.receiveMessage[IntermediateSearchResult] {

      case SearchQueryResponse(_, _, replyTo) =>
        replyTo ! SearchFailure
        Behaviors.same

      case FetchQueryResponse(_, replyTo) =>
        replyTo ! SearchFailure
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}
