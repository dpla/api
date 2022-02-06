package dpla.ebookapi.mocks

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.helpers.FileReader
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.{GetEsFetchResult, GetEsSearchResult}
import dpla.ebookapi.v1.ebooks.{ElasticSearchClient, ElasticSearchUnreachable}

import scala.concurrent.ExecutionContextExecutor

object MockEsClientUnreachable extends FileReader {

  def apply(): Behavior[ElasticSearchClient.EsClientCommand] = {
    Behaviors.setup { context =>
      implicit val executor: ExecutionContextExecutor = context.executionContext

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
}
