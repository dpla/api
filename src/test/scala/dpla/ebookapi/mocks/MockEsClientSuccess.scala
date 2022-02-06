package dpla.ebookapi.mocks

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import dpla.ebookapi.helpers.FileReader
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.{GetEsFetchResult, GetEsSearchResult}
import dpla.ebookapi.v1.ebooks.{ElasticSearchClient, ElasticSearchSuccess}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.OK

import scala.concurrent.{ExecutionContextExecutor, Future}

object MockEsClientSuccess extends FileReader {

  private val searchBody: String = readFile("/elasticSearchMinimalEbookList.json")
  private val fetchBody: String = readFile("/elasticSearchMinimalEbook.json")

  def apply(): Behavior[ElasticSearchClient.EsClientCommand] = {
    Behaviors.setup { context =>
      implicit val executor: ExecutionContextExecutor = context.executionContext

      Behaviors.receiveMessage[ElasticSearchClient.EsClientCommand] {

        case GetEsSearchResult(_, replyTo) =>
//          val searchResponse = Future(HttpResponse(OK, entity=searchBody))
          replyTo ! ElasticSearchSuccess(searchBody)
          Behaviors.same

        case GetEsFetchResult(_, replyTo) =>
//          val fetchResponse = Future(HttpResponse(OK, entity=fetchBody))
          replyTo ! ElasticSearchSuccess(fetchBody)
          Behaviors.same
      }
    }
  }
}
