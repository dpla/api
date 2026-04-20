package dpla.api.v2.search

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.api.v2.search.SearchProtocol.{
  IntermediateSearchResult,
  RandomQuery,
  SearchQuery,
  SearchQueryParseFailure
}

object MockEsClientQueryParseError {

  def apply(): Behavior[IntermediateSearchResult] = {
    Behaviors.receiveMessage[IntermediateSearchResult] {

      case SearchQuery(_, _, replyTo) =>
        replyTo ! SearchQueryParseFailure
        Behaviors.same

      case RandomQuery(_, _, replyTo) =>
        replyTo ! SearchQueryParseFailure
        Behaviors.same

      case _ =>
        Behaviors.unhandled
    }
  }
}
