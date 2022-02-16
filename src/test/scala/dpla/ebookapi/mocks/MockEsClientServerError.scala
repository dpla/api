package dpla.ebookapi.mocks

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.{EsClientCommand, GetEsFetchResult, GetEsSearchResult}
import dpla.ebookapi.v1.ebooks.ElasticSearchHttpFailure

object MockEsClientServerError {

  def apply(): Behavior[EsClientCommand] = {
    Behaviors.receiveMessage[EsClientCommand] {

      case GetEsSearchResult(_, replyTo) =>
        replyTo ! ElasticSearchHttpFailure(500)
        Behaviors.same

      case GetEsFetchResult(_, replyTo) =>
        replyTo ! ElasticSearchHttpFailure(500)
        Behaviors.same
    }
  }
}
