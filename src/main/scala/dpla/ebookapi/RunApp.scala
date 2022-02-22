package dpla.ebookapi

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import dpla.ebookapi.v1.PostgresClient
import dpla.ebookapi.v1.apiKey.ApiKeyRegistry
import dpla.ebookapi.v1.ebooks.{EbookRegistry, ElasticSearchClient}

import scala.util.{Failure, Success}

//#main-class
object RunApp {
  //#start-http-server
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext

    val futureBinding =
      Http().newServerAt("0.0.0.0", 8080).bind(routes)

    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(
          "Server online at http://{}:{}/",
          address.getHostString, address.getPort
        )
      case Failure(e) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", e)
        system.terminate()
    }
  }

  //#start-http-server
  def main(args: Array[String]): Unit = {

    //#server-bootstrapping
    val rootBehavior = Behaviors.setup[Nothing] { context =>

      // Spawn top-level actors.

      val elasticSearchClient =
        context.spawn(
          ElasticSearchClient(),
          "ElasticSearchClient"
        )

      val postgresClient =
        context.spawn(PostgresClient(), "PostgresClient")

      val ebookRegistry =
        context.spawn(
          EbookRegistry(elasticSearchClient, postgresClient),
          "EbookRegistry"
        )

      val apiKeyRegistry =
        context.spawn(
          ApiKeyRegistry(postgresClient),
          "ApiKeyRegistry"
        )

      context.watch(ebookRegistry)
      context.watch(apiKeyRegistry)
      context.watch(elasticSearchClient)
      context.watch(postgresClient)

      // Start the HTTP server.
      val routes = new Routes(ebookRegistry, apiKeyRegistry)(context.system)

      startHttpServer(routes.applicationRoutes)(context.system)

      Behaviors.empty
    }
    ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
    //#server-bootstrapping
  }
}
