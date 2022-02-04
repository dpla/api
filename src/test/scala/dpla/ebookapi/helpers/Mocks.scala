//package dpla.ebookapi.helpers
//
//import akka.actor.typed.ActorSystem
//import akka.actor.typed.scaladsl.Behaviors
//import akka.http.scaladsl.model.{StatusCode, StatusCodes}
//import dpla.ebookapi.v1.ebooks.{Ebook, EbookList, OldElasticSearchClient, FieldFilter, SearchParams, SingleEbook}
//import spray.json.DeserializationException
//
//import java.net.UnknownHostException
//import scala.concurrent.{ExecutionContextExecutor, Future}
//
//trait Mocks {
//
//  def getMockElasticSearchClient: MockElasticSearchClient =
//    new MockElasticSearchClient("http://fake-endpoint.com")
//
//  def getMockElasticSearchClientParseError: MockElasticSearchClientParseError =
//    new MockElasticSearchClientParseError("http://fake-endpoint.com")
//
//  def getMockElasticSearchClientNotFound: MockElasticSearchClientNotFound =
//    new MockElasticSearchClientNotFound("http://fake-endpoint.com")
//
//  def getMockElasticSearchClientUnexpectedError: MockElasticSearchClientUnexpectedError =
//    new MockElasticSearchClientUnexpectedError("http://fake-endpoint.com")
//
//  def getMockElasticSearchClientFailedRequest: MockElasticSearchClientFailedRequest =
//    new MockElasticSearchClientFailedRequest("http://fake-endpoint.com")
//
//  class MockElasticSearchClient(elasticSearchEndpoint: String)
//    extends OldElasticSearchClient(elasticSearchEndpoint: String) {
//
//    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "MockElasticSearch")
//    implicit val executionContext: ExecutionContextExecutor = system.executionContext
//
//    val ebookList: EbookList = EbookList(None, None, None, Seq[Ebook](), None)
//    val singleEbook: SingleEbook = SingleEbook(Seq[Ebook]())
//
//    val minSearchParams: SearchParams = SearchParams(
//      exactFieldMatch = false,
//      facets = None,
//      facetSize = 0,
//      filters = Seq[FieldFilter](),
//      page = 0,
//      pageSize = 0,
//      q = None
//    )
//
//    // These allow you to check that specific params were passed to the client in tests.
//    private var lastParams: SearchParams = minSearchParams
//    private var lastId: String = ""
//
//    def getLastParams: SearchParams = lastParams
//    def getLastId: String = lastId
//
//    override def search(params: SearchParams): Future[Either[StatusCode, Future[EbookList]]] = {
//      lastParams = params
//      Future(Right(Future(ebookList)))
//    }
//
//    override def fetch(id: String): Future[Either[StatusCode, Future[SingleEbook]]] = {
//      lastId = id
//      Future(Right(Future(singleEbook)))
//    }
//  }
//
//  class MockElasticSearchClientParseError(elasticSearchEndpoint: String)
//    extends OldElasticSearchClient(elasticSearchEndpoint: String) {
//
//    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "MockElasticSearch")
//    implicit val executionContext: ExecutionContextExecutor = system.executionContext
//
//    override def search(params: SearchParams): Future[Either[StatusCode, Future[EbookList]]] =
//      Future(Right(Future.failed(DeserializationException(""))))
//
//    override def fetch(id: String): Future[Either[StatusCode, Future[SingleEbook]]] =
//      Future(Right(Future.failed(DeserializationException(""))))
//  }
//
//  class MockElasticSearchClientNotFound(elasticSearchEndpoint: String)
//    extends OldElasticSearchClient(elasticSearchEndpoint: String) {
//
//    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "MockElasticSearch")
//    implicit val executionContext: ExecutionContextExecutor = system.executionContext
//
//    override def fetch(id: String): Future[Either[StatusCode, Future[SingleEbook]]] =
//      Future(Left(StatusCodes.NotFound))
//  }
//
//  class MockElasticSearchClientUnexpectedError(elasticSearchEndpoint: String)
//    extends OldElasticSearchClient(elasticSearchEndpoint: String) {
//
//    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "MockElasticSearch")
//    implicit val executionContext: ExecutionContextExecutor = system.executionContext
//
//    override def search(params: SearchParams): Future[Either[StatusCode, Future[EbookList]]] =
//      Future(Left(StatusCodes.MethodNotAllowed))
//
//    override def fetch(id: String): Future[Either[StatusCode, Future[SingleEbook]]] =
//      Future(Left(StatusCodes.MethodNotAllowed))
//  }
//
//  class MockElasticSearchClientFailedRequest(elasticSearchEndpoint: String)
//    extends OldElasticSearchClient(elasticSearchEndpoint: String) {
//
//    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "MockElasticSearch")
//    implicit val executionContext: ExecutionContextExecutor = system.executionContext
//
//    override def search(params: SearchParams): Future[Either[StatusCode, Future[EbookList]]] =
//      Future.failed(new UnknownHostException)
//
//    override def fetch(id: String): Future[Either[StatusCode, Future[SingleEbook]]] =
//      Future.failed(new UnknownHostException)
//  }
//}
