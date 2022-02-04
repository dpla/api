//package dpla.ebookapi.v1.ebooks
//
//import akka.actor.testkit.typed.scaladsl.ActorTestKit
//import akka.actor.typed.ActorSystem
//import akka.http.scaladsl.model._
//import akka.http.scaladsl.model.headers.Accept
//import akka.http.scaladsl.server.Route
//import akka.http.scaladsl.testkit.ScalatestRouteTest
//import dpla.ebookapi.Routes
//import dpla.ebookapi.helpers.Mocks
//import org.scalatest.matchers.should.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//
//class ElasticSearchErrorTest extends AnyWordSpec with Matchers with ScalatestRouteTest with Mocks {
//
//  lazy val testKit: ActorTestKit = ActorTestKit()
//  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
//  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.classicSystem
//
//  "/v1/ebooks route" should {
//    "return Teapot if ElasticSearch response cannot be parsed" in {
//      lazy val routes: Route = new Routes(getMockElasticSearchClientParseError).applicationRoutes
//
//      val request = Get("/v1/ebooks")
//        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/xml`))))
//
//      request ~> Route.seal(routes) ~> check {
//        status shouldEqual StatusCodes.ImATeapot
//      }
//    }
//
//    "return Teapot if ElasticSearch returns unexpected error" in {
//      lazy val routes: Route = new Routes(getMockElasticSearchClientUnexpectedError).applicationRoutes
//
//      val request = Get("/v1/ebooks")
//        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/xml`))))
//
//      request ~> Route.seal(routes) ~> check {
//        status shouldEqual StatusCodes.ImATeapot
//      }
//    }
//
//    "return Teapot if call to ElasticSearch fails" in {
//      lazy val routes: Route = new Routes(getMockElasticSearchClientFailedRequest).applicationRoutes
//
//      val request = Get("/v1/ebooks")
//        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/xml`))))
//
//      request ~> Route.seal(routes) ~> check {
//        status shouldEqual StatusCodes.ImATeapot
//      }
//    }
//  }
//
//  "/v1/ebooks[id] route" should {
//    "return Teapot if ElasticSearch response cannot be parsed" in {
//      lazy val routes: Route = new Routes(getMockElasticSearchClientParseError).applicationRoutes
//
//      val request = Get("/v1/ebooks/R0VfVX4BfY91SSpFGqxt")
//        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/xml`))))
//
//      request ~> Route.seal(routes) ~> check {
//        status shouldEqual StatusCodes.ImATeapot
//      }
//    }
//
//    "return Not Found if ebook not found" in {
//      lazy val routes: Route = new Routes(getMockElasticSearchClientNotFound).applicationRoutes
//
//      val request = Get("/v1/ebooks/R0VfVX4BfY91SSpFGqxt")
//        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/xml`))))
//
//      request ~> Route.seal(routes) ~> check {
//        status shouldEqual StatusCodes.NotFound
//      }
//    }
//
//    "return Teapot if ElasticSearch returns unexpected error" in {
//      lazy val routes: Route = new Routes(getMockElasticSearchClientUnexpectedError).applicationRoutes
//
//      val request = Get("/v1/ebooks/R0VfVX4BfY91SSpFGqxt")
//        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/xml`))))
//
//      request ~> Route.seal(routes) ~> check {
//        status shouldEqual StatusCodes.ImATeapot
//      }
//    }
//
//    "return Teapot if call to ElasticSearch fails" in {
//      lazy val routes: Route = new Routes(getMockElasticSearchClientFailedRequest).applicationRoutes
//
//      val request = Get("/v1/ebooks/R0VfVX4BfY91SSpFGqxt")
//        .withHeaders(Accept(Seq(MediaRange(MediaTypes.`application/xml`))))
//
//      request ~> Route.seal(routes) ~> check {
//        status shouldEqual StatusCodes.ImATeapot
//      }
//    }
//  }
//}
