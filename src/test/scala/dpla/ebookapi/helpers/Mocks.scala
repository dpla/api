package dpla.ebookapi.helpers

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.StatusCode
import dpla.ebookapi.v1.ebooks.{Ebook, EbookList, ElasticSearchClient, FieldFilter, RawParams, SearchParams, SingleEbook}

import scala.concurrent.{ExecutionContextExecutor, Future}

trait Mocks {

  def getMockElasticSearchClient: MockElasticSearchClient =
    new MockElasticSearchClient("http://fake-endpoint.com")

  class MockElasticSearchClient(elasticSearchEndpoint: String)
    extends ElasticSearchClient(elasticSearchEndpoint: String) {

    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "MockElasticSearch")
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    val ebookList: EbookList = EbookList(None, None, None, Seq[Ebook](), None)
    val singleEbook: SingleEbook = SingleEbook(Seq[Ebook]())

    val minSearchParams: SearchParams = SearchParams(
      exactFieldMatch = false,
      facets = None,
      facetSize = 0,
      filters = Seq[FieldFilter](),
      page = 0,
      pageSize = 0,
      q = None
    )

    // These allow you to check that specific params were passed to the client in tests.
    private var lastParams: SearchParams = minSearchParams
    private var lastId: String = ""

    def getLastParams: SearchParams = lastParams
    def getLastId: String = lastId

    override def search(params: SearchParams): Future[Either[StatusCode, Future[EbookList]]] = {
      lastParams = params
      Future(Right(Future(ebookList)))
    }

    override def fetch(id: String): Future[Either[StatusCode, Future[SingleEbook]]] = {
      lastId = id
      Future(Right(Future(singleEbook)))
    }
  }
}
