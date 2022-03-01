package dpla.ebookapi.v1

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import dpla.ebookapi.v1.ebooks.EbookList


/**
 * Tracks use via Google Analytics Measurement Protocol
 * @see https://developers.google.com/analytics/devguides/collection/protocol/v1
 *
 * Google Analytics Measurement Protocol does not return HTTP codes, so the
 * success or failure of a request cannot be ascertained.
 */
object AnalyticsClient {

  sealed trait AnalyticsClientCommand

  case class TrackSearch(
                          rawParams: Map[String, String],
                          host: String,
                          path: String,
                          ebookList: EbookList
                        ) extends AnalyticsClientCommand

  case class TrackFetch(

                       ) extends AnalyticsClientCommand

  def apply(): Behavior[AnalyticsClientCommand] = {
    Behaviors.setup{ context =>

      implicit val system: ActorSystem[Nothing] = context.system

      val collectUrl = "https://www.google-analytics.com/collect"
      val batchUrl = "http://www.google-analytics.com/batch"

      val trackingId: String = context.system.settings.config
        .getString("googleAnalytics.trackingId")

      Behaviors.receiveMessage[AnalyticsClientCommand] {

        case TrackSearch(rawParams, host, path, ebookList) =>
          rawParams.get("api_key") match {
            case Some(apiKey) =>
              val query = rawParams
                .filterNot(_._1 == "api_key")
                .map{ case(key, value) => s"$key=$value" }
                .mkString("&")
              val fullPath = s"$path?$query"
              val hitType = "pageview"
              val title = "Ebook search result"


              val data = Map(
                "v" -> "1",
                "tid" -> trackingId,
                "t" -> hitType,
                "dh" -> host,
                "dp" -> fullPath,
                "dt" -> title,
                "cid" -> apiKey
              ).map{case (key, value) => s"$key=$value" }
                .mkString("&")

              val request: HttpRequest = HttpRequest(
                method = HttpMethods.POST,
                entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, data),
                uri = collectUrl
              )

              Http().singleRequest(request)

            case None =>
              // no-op
          }




          Behaviors.same
      }
    }
  }
}
