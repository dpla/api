package dpla.ebookapi

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Route
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.ebookapi.v1.ebooks.ElasticSearchClient

class HealthCheckTest extends AnyWordSpec with Matchers with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()
  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.classicSystem

  val elasticSearchClient = new ElasticSearchClient("http://es-endpoint.com")
  lazy val routes: Route = new Routes(elasticSearchClient).applicationRoutes

  "Health check" should {
    "return OK" in {
      val request = HttpRequest(uri="/health-check")

      request ~> routes ~> check {
        status should === (StatusCodes.OK)
      }
    }
  }
}
