package dpla.ebookapi.mocks

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.{GetEsFetchResult, GetEsSearchResult}
import dpla.ebookapi.v1.ebooks.{ElasticSearchClient, ElasticSearchUnreachable}

object MockEsClientUnreachable {

  def apply(): Behavior[ElasticSearchClient.EsClientCommand] = {
    Behaviors.receiveMessage[ElasticSearchClient.EsClientCommand] {

      case GetEsSearchResult(_, replyTo) =>
        replyTo ! ElasticSearchUnreachable
        Behaviors.same

      case GetEsFetchResult(_, replyTo) =>
        replyTo ! ElasticSearchUnreachable
        Behaviors.same
    }
  }
}
