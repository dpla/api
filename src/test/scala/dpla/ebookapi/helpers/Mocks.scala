package dpla.ebookapi.helpers

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.StatusCode
import dpla.ebookapi.v1.ebooks.{Ebook, EbookList, ElasticSearchClient, SearchParams, SingleEbook}

import scala.concurrent.{ExecutionContextExecutor, Future}

trait Mocks {

  def getMockElasticSearchClient: ElasticSearchClient =
    new MockElasticSearchClient("http://fake-endpoint.com")

  class MockElasticSearchClient(elasticSearchEndpoint: String)
    extends ElasticSearchClient(elasticSearchEndpoint: String) {

    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "MockElasticSearch")
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    val ebookList: EbookList = EbookList(None, None, None, Seq[Ebook](), None)
    val singleEbook: SingleEbook = SingleEbook(Seq[Ebook]())

    override def search(params: SearchParams): Future[Either[StatusCode, Future[EbookList]]] =
      Future(Right(Future(ebookList)))

    override def fetch(id: String): Future[Either[StatusCode, Future[SingleEbook]]] =
      Future(Right(Future(singleEbook)))
  }

}
