package dpla.ebookapi.v1

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import dpla.ebookapi.v1.ebooks.{EbookList, SingleEbook}


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
                          apiKey: String,
                          rawParams: Map[String, String],
                          host: String,
                          path: String,
                          ebookList: EbookList
                        ) extends AnalyticsClientCommand

  case class TrackFetch(
                         apiKey: String,
                         host: String,
                         path: String,
                         singleEbook: SingleEbook
                       ) extends AnalyticsClientCommand

  def apply(): Behavior[AnalyticsClientCommand] = {
    Behaviors.setup{ context =>

      implicit val system: ActorSystem[Nothing] = context.system

      val collectUrl = "https://www.google-analytics.com/collect"
      val batchUrl = "http://www.google-analytics.com/batch"

      val trackingId: String = context.system.settings.config
        .getString("googleAnalytics.trackingId")

      Behaviors.receiveMessage[AnalyticsClientCommand] {

        case TrackSearch(apiKey, rawParams, host, path, ebookList) =>

          val title = "Ebook search results"
          val query: String = paramString(rawParams.filterNot(_._1 == "api_key"))
          val fullPath = s"$path?$query"

          val data = Map(
            "v" -> "1",
            "tid" -> trackingId,
            "t" -> "pageview",
            "dh" -> host,
            "dp" -> fullPath,
            "dt" -> title,
            "cid" -> apiKey
          )
          val dataString = paramString(data)

          val request: HttpRequest = HttpRequest(
            method = HttpMethods.POST,
            entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, dataString),
            uri = collectUrl
          )
          Http().singleRequest(request)

          Behaviors.same

        case TrackFetch(apiKey, host, path, singleEbook) =>

          val title = "Fetch ebook"

          val data = Map(
            "v" -> "1",
            "tid" -> trackingId,
            "t" -> "pageview",
            "dh" -> host,
            "dp" -> path,
            "dt" -> title,
            "cid" -> apiKey
          )
          val dataString = paramString(data)

          val request: HttpRequest = HttpRequest(
            method = HttpMethods.POST,
            entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, dataString),
            uri = collectUrl
          )
          Http().singleRequest(request)

          Behaviors.same
      }
    }
  }

  // Turn a param map into a string that can be used in an HTTP request
  private def paramString(params: Map[String, String]): String =
    params.map{case (key, value) => s"$key=$value" }.mkString("&")
}
