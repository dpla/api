package dpla.ebookapi.v1

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import dpla.ebookapi.v1.search.Ebook


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
                          ebooks: Seq[Ebook]
                        ) extends AnalyticsClientCommand

  case class TrackFetch(
                         apiKey: String,
                         host: String,
                         path: String,
                         ebook: Option[Ebook]
                       ) extends AnalyticsClientCommand

  val collectUrl = "https://www.google-analytics.com/collect"
  val batchUrl = "http://www.google-analytics.com/batch"

  def apply(): Behavior[AnalyticsClientCommand] = {
    Behaviors.setup{ context =>

      implicit val system: ActorSystem[Nothing] = context.system

      val trackingId: String = system.settings.config
        .getString("googleAnalytics.trackingId")

      Behaviors.receiveMessage[AnalyticsClientCommand] {

        case TrackSearch(apiKey, rawParams, host, path, ebooks) =>

          // Track pageview
          // Strip the API key out of the page path
          val query: String = paramString(rawParams.filterNot(_._1 == "api_key"))
          val pageViewParams: String = trackPageViewParams(
            trackingId,
            apiKey,
            host,
            s"$path?$query",
            "Ebook search results"
          )
          postHit(system, pageViewParams)

          // Track events
          if (ebooks.nonEmpty) {
            val eventParams: Seq[String] = ebooks.map(ebook =>
              trackEventParams(
                trackingId,
                apiKey,
                host,
                path,
                ebookEventCategory(ebook),
                ebookEventAction(ebook),
                ebookEventLabel(ebook)
              )
            )
            postBatch(system, eventParams)
          }

          Behaviors.same

        case TrackFetch(apiKey, host, path, ebook) =>

          // Track pageview
          val pageViewParams: String = trackPageViewParams(
            trackingId,
            apiKey,
            host,
            path,
            "Fetch ebooks"
          )
          postHit(system, pageViewParams)

          // Track event
          ebook match {
            case Some(ebook) =>
              val eventParams: String = trackEventParams(
                trackingId,
                apiKey,
                host,
                path,
                ebookEventCategory(ebook),
                ebookEventAction(ebook),
                ebookEventLabel(ebook)
              )
              postHit(system, eventParams)
            case None => // no-op
          }

          Behaviors.same
      }
    }
  }

  private def trackPageViewParams(
                                   trackingId: String,
                                   apiKey: String,
                                   host: String,
                                   path: String,
                                   title: String
                                  ): String = {
    val params = Map(
      "v" -> "1",
      "t" -> "pageview",
      "tid" -> trackingId,
      "cid" -> apiKey,
      "dh" -> host,
      "dp" -> path,
      "dt" -> title
    )
    paramString(params)
  }

  private def trackEventParams(
                                trackingId: String,
                                apiKey: String,
                                host: String,
                                path: String,
                                category: String,
                                action: String,
                                label: String
                              ): String = {
    val params = Map(
      "v" -> "1",
      "t" -> "event",
      "tid" -> trackingId,
      "cid" -> apiKey,
      "dh" -> host,
      "dp" -> path,
      "ec" -> category,
      "ea" -> action,
      "el" -> label
    )
    paramString(params)
  }

  private def ebookEventCategory(ebook: Ebook): String = {
    val provider = ebook.providerName.getOrElse("")
    s"View API Ebook : $provider"
  }

  private def ebookEventAction(ebook: Ebook): String =
    ebook.providerName.getOrElse("")

  private def ebookEventLabel(ebook: Ebook): String = {
    val docId = ebook.id.getOrElse("")
    val title = ebook.title.mkString(", ")
    s"$docId : $title"
  }

  private def postHit(
                       implicit system: ActorSystem[Nothing],
                       data: String
                     ): Unit = {

    val request: HttpRequest = HttpRequest(
      method = HttpMethods.POST,
      entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, data),
      uri = collectUrl
    )
    Http().singleRequest(request)
  }

  private def postBatch(
                         implicit system: ActorSystem[Nothing],
                         data: Seq[String]
                       ): Unit = {

    // Can only send up to 20 hits per batch
    val batches: Iterator[Seq[String]] = data.grouped(20)

    for (batch <- batches) {
      val data = batch.mkString(System.lineSeparator)
      val request: HttpRequest = HttpRequest(
        method = HttpMethods.POST,
        entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, data),
        uri = batchUrl
      )
      Http().singleRequest(request)
    }
  }

  // Turn a param map into a string that can be used in an HTTP request
  private def paramString(params: Map[String, String]): String =
    params.map{case (key, value) => s"$key=$value" }.mkString("&")
}
