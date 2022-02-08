package dpla.ebookapi

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import dpla.ebookapi.v1.ebooks.{EbookRegistry, ElasticSearchClient}

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

    //#server-bootstrapping
    val rootBehavior = Behaviors.setup[Nothing] { context =>

      val elasticSearchEndpoint: String = System.getenv("ELASTICSEARCH_URL") match {
        case "" => "http://localhost:9200/eleanor"
        case x => x.stripSuffix("/")
      }

      // Spawn EbookRegistry and ElasticSearchClient, the two top-level actors.
      val ebookRegistry = context.spawn(EbookRegistry(), "EbookRegistry")
      context.watch(ebookRegistry)

      val elasticSearchClient = context.spawn(ElasticSearchClient(elasticSearchEndpoint), "ElasticSearchClient")
      context.watch(elasticSearchClient)

      // Start the HTTP server. Pass references to the EbookRegistry and ElasticSearchClient actors to Routes.
      val routes = new Routes(ebookRegistry, elasticSearchClient)(context.system)
      startHttpServer(routes.applicationRoutes)(context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
    //#server-bootstrapping
  }
}
