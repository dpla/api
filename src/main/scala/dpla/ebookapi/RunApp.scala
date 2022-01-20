package dpla.ebookapi

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import dpla.ebookapi.v1.ebooks.ElasticSearchClient

import scala.util.{Failure, Success}

//#main-class
object RunApp {
  //#start-http-server
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext

    val futureBinding = Http().newServerAt("0.0.0.0", 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  //#start-http-server
  def main(args: Array[String]): Unit = {

    val elasticSearchEndpoint: String = System.getenv("ELASTICSEARCH_URL") match {
      case "" => "http://localhost:9200/eleanor"
      case x => x
    }

    val elasticSearchClient = new ElasticSearchClient(elasticSearchEndpoint)

    //#server-bootstrapping
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val routes = new Routes(elasticSearchClient)(context.system)
      startHttpServer(routes.applicationRoutes)(context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
    //#server-bootstrapping
  }
}
