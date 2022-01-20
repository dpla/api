package dpla.ebookapi.v1.ebooks

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.Routes
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future

class PermittedMediaTypesTest extends AnyWordSpec with Matchers with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()
  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.classicSystem

  class MockElasticSearchClient(elasticSearchEndpoint: String)
    extends ElasticSearchClient(elasticSearchEndpoint: String) {

    val ebookList: EbookList = EbookList(None, None, None, Seq[Ebook](), None)
    val singleEbook: SingleEbook = SingleEbook(Seq[Ebook]())

    override def search(params: SearchParams): Future[Either[StatusCode, Future[EbookList]]] =
      Future(Right(Future(ebookList)))

    override def fetch(id: String): Future[Either[StatusCode, Future[SingleEbook]]] =
      Future(Right(Future(singleEbook)))
  }

  val elasticSearchClient = new MockElasticSearchClient("http://es-endpoint.com")
  lazy val routes: Route = new Routes(elasticSearchClient).applicationRoutes

  "/v1/ebooks route" should {
    "reject invalid media types" in {

      val request = Get("/v1/ebooks")
        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/xml`))))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.NotAcceptable
      }
    }

    "allow valid media type" in {

      val request = Get("/v1/ebooks")
        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/json`))))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }

  "/v1/ebooks[id] route" should {
    "reject invalid media types" in {

      val request = Get("/v1/ebooks/R0VfVX4BfY91SSpFGqxt")
        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/xml`))))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.NotAcceptable
      }
    }

    "allow valid media type" in {

      val request = Get("/v1/ebooks/R0VfVX4BfY91SSpFGqxt")
        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/json`))))

      request ~> Route.seal(routes) ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }
}
