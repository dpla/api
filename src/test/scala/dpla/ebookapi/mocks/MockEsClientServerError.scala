package dpla.ebookapi.mocks

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.{GetEsFetchResult, GetEsSearchResult}
import dpla.ebookapi.v1.ebooks.{ElasticSearchClient, ElasticSearchHttpFailure}

object MockEsClientServerError {

  def apply(): Behavior[ElasticSearchClient.EsClientCommand] = {
    Behaviors.receiveMessage[ElasticSearchClient.EsClientCommand] {

      case GetEsSearchResult(_, replyTo) =>
        replyTo ! ElasticSearchHttpFailure(500)
        Behaviors.same

      case GetEsFetchResult(_, replyTo) =>
        replyTo ! ElasticSearchHttpFailure(500)
        Behaviors.same
    }
  }
}
