package dpla.ebookapi.mocks

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.{EsClientCommand, GetEsFetchResult, GetEsSearchResult}
import dpla.ebookapi.v1.ebooks.ElasticSearchUnreachable

object MockEsClientUnreachable {

  def apply(): Behavior[EsClientCommand] = {
    Behaviors.receiveMessage[EsClientCommand] {

      case GetEsSearchResult(_, replyTo) =>
        replyTo ! ElasticSearchUnreachable
        Behaviors.same

      case GetEsFetchResult(_, replyTo) =>
        replyTo ! ElasticSearchUnreachable
        Behaviors.same
    }
  }
}
