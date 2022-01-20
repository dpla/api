package dpla.ebookapi

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Route
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.v1.ebooks.ElasticSearchClient

class PermittedHttpMethodsTest extends AnyWordSpec with Matchers with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()
  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.classicSystem

  val elasticSearchClient = new ElasticSearchClient("http://es-endpoint.com")
  lazy val routes: Route = new Routes(elasticSearchClient).applicationRoutes

  "/ebooks route" should {
    "handle invalid HTTP methods" should {
      "reject DELETE" in {
        val request = Delete("/ebooks")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject PATCH" in {
        val request = Patch("/ebooks")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject POST" in {
        val request = Post("/ebooks")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject PUT" in {
        val request = Put("/ebooks")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }
    }
  }

  "/ebooks/[id] route" should {
    "handle invalid HTTP methods" should {
      "reject DELETE" in {
        val request = Delete("/ebooks/R0VfVX4BfY91SSpFGqxt")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject PATCH" in {
        val request = Patch("/ebooks/R0VfVX4BfY91SSpFGqxt")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject POST" in {
        val request = Post("/ebooks/R0VfVX4BfY91SSpFGqxt")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }

      "reject PUT" in {
        val request = Put("/ebooks/R0VfVX4BfY91SSpFGqxt")

        request ~> Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.MethodNotAllowed
        }
      }
    }
  }
}
