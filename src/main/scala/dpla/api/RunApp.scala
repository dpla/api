package dpla.api

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import dpla.api.v2.analytics._
import dpla.api.v2.authentication.AuthProtocol.AuthenticationCommand
import dpla.api.v2.authentication.Authenticator
import dpla.api.v2.registry._

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
      val authenticator: ActorRef[AuthenticationCommand] =
        context.spawn(Authenticator(), "Authenticator")

      val ebookAnalyticsClient: ActorRef[AnalyticsClientCommand] =
        context.spawn(EbookAnalyticsClient(), "EbookAnalyticsClient")

      val itemAnalyticsClient: ActorRef[AnalyticsClientCommand] =
        context.spawn(ItemAnalyticsClient(), "ItemAnalyticsClient")

      val pssAnalyticsClient: ActorRef[AnalyticsClientCommand] =
        context.spawn(PssAnalyticsClient(), "PssAnalyticsClient")

      val ebookRegistry: ActorRef[SearchRegistryCommand] =
        context.spawn(
          EbookRegistry(authenticator, ebookAnalyticsClient), "EbookRegistry"
        )

      val itemRegistry: ActorRef[SearchRegistryCommand] =
        context.spawn(
          ItemRegistry(authenticator, itemAnalyticsClient), "ItemRegistry"
        )

      val pssRegistry: ActorRef[SearchRegistryCommand] =
        context.spawn(
          PssRegistry(authenticator, pssAnalyticsClient), name = "PssRegsitry"
        )

      val smrRegistry: ActorRef[SmrRegistryCommand] =
        context.spawn(
          SmrRegistry(authenticator), name = "SmrRegistry"
        )

      val apiKeyRegistry: ActorRef[ApiKeyRegistryCommand] =
        context.spawn(ApiKeyRegistry(authenticator), "ApiKeyRegistry")

      context.watch(ebookRegistry)
      context.watch(itemRegistry)
      context.watch(pssRegistry)
      context.watch(apiKeyRegistry)
      context.watch(smrRegistry)

      // Start the HTTP server.
      val routes =
        new Routes(
          ebookRegistry,
          itemRegistry,
          pssRegistry,
          apiKeyRegistry,
          smrRegistry
        )(context.system)

      startHttpServer(routes.applicationRoutes)(context.system)

      Behaviors.empty
    }
    ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
    //#server-bootstrapping
  }
}
