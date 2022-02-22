package dpla.ebookapi.mocks

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.helpers.FileReader
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.{EsClientCommand, GetEsFetchResult, GetEsSearchResult}
import dpla.ebookapi.v1.ebooks.ElasticSearchSuccess

object MockEsClientSuccess extends FileReader {

  private val searchBody: String =
    readFile("/elasticSearchEbookList.json")

  private val fetchBody: String =
    readFile("/elasticSearchEbook.json")

  def apply(): Behavior[EsClientCommand] = {
    Behaviors.receiveMessage[EsClientCommand] {

      case GetEsSearchResult(_, replyTo) =>
        replyTo ! ElasticSearchSuccess(searchBody)
        Behaviors.same

      case GetEsFetchResult(_, replyTo) =>
        replyTo ! ElasticSearchSuccess(fetchBody)
        Behaviors.same
    }
  }
}
