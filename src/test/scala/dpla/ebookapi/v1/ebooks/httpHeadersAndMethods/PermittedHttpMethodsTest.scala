package dpla.ebookapi.v1.ebooks.httpHeadersAndMethods

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.Routes
import dpla.ebookapi.mocks.{MockEsClientSuccess, MockPostgresClientSuccess}
import dpla.ebookapi.v1.PostgresClient.PostgresClientCommand
import dpla.ebookapi.v1.ebooks.EbookRegistry
import dpla.ebookapi.v1.ebooks.ElasticSearchClient.EsClientCommand
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PermittedHttpMethodsTest extends AnyWordSpec with Matchers
  with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  implicit def typedSystem: ActorSystem[Nothing] =
    testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem
  val postgresClient: ActorRef[PostgresClientCommand] =
    testKit.spawn(MockPostgresClientSuccess())
  val elasticSearchClient: ActorRef[EsClientCommand] =
    testKit.spawn(MockEsClientSuccess())
  val ebookRegistry: ActorRef[EbookRegistry.RegistryCommand] =
    testKit.spawn(EbookRegistry(elasticSearchClient, postgresClient))
  lazy val routes: Route =
    new Routes(ebookRegistry).applicationRoutes

  "/v1/ebooks route" should {
    "handle invalid HTTP methods" should {
      "reject DELETE" in {
        val request = Delete("/v1/ebooks")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject PATCH" in {
        val request = Patch("/v1/ebooks")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject POST" in {
        val request = Post("/v1/ebooks")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject PUT" in {
        val request = Put("/v1/ebooks")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }
    }
  }

  "/v1/ebooks/[id] route" should {
    "handle invalid HTTP methods" should {
      "reject DELETE" in {
        val request = Delete("/v1/ebooks/R0VfVX4BfY91SSpFGqxt")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject PATCH" in {
        val request = Patch("/v1/ebooks/R0VfVX4BfY91SSpFGqxt")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject POST" in {
        val request = Post("/v1/ebooks/R0VfVX4BfY91SSpFGqxt")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject PUT" in {
        val request = Put("/v1/ebooks/R0VfVX4BfY91SSpFGqxt")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }
    }
  }
}
