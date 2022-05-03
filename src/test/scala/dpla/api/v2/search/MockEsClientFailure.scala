package dpla.api.v2.search

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.api.v2.search.SearchProtocol.{FetchQuery, IntermediateSearchResult, SearchFailure, SearchQuery}

object MockEsClientFailure {

  def apply(): Behavior[IntermediateSearchResult] = {
    Behaviors.receiveMessage[IntermediateSearchResult] {

      case SearchQuery(_, _, replyTo) =>
        replyTo ! SearchFailure
        Behaviors.same

      case FetchQuery(_, replyTo) =>
        replyTo ! SearchFailure
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}
