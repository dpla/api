package dpla.ebookapi.mocks

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.{EsClientCommand, GetEsFetchResult, GetEsSearchResult}
import dpla.ebookapi.v1.ebooks.ElasticSearchParseFailure


object MockEsClientParseError {

  def apply(): Behavior[EsClientCommand] = {
    Behaviors.receiveMessage[EsClientCommand] {

      case GetEsSearchResult(_, replyTo) =>
        replyTo ! ElasticSearchParseFailure
        Behaviors.same

      case GetEsFetchResult(_, replyTo) =>
        replyTo ! ElasticSearchParseFailure
        Behaviors.same
    }
  }

}
