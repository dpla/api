package dpla.api.v2.analytics

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import dpla.api.v2.search.JsonFieldReader
import spray.json.JsValue

/**
 * Tracks use via Google Analytics Measurement Protocol
 *
 * @see https://developers.google.com/analytics/devguides/collection/protocol/v1
 *
 * Google Analytics Measurement Protocol does not return HTTP codes, so the
 * success or failure of a request cannot be ascertained.
 */
object AnalyticsClient extends JsonFieldReader {

  sealed trait AnalyticsClientCommand

  case class TrackSearch(
                          rawParams: Map[String, String],
                          host: String,
                          path: String,
                          dplaDocList: Seq[JsValue],
                          searchType: String
                        ) extends AnalyticsClientCommand

  case class TrackFetch(
                         host: String,
                         path: String,
                         dplaDoc: Option[JsValue],
                         searchType: String
                       ) extends AnalyticsClientCommand

  val collectUrl = "https://www.google-analytics.com/collect"
  val batchUrl = "http://www.google-analytics.com/batch"

  def apply(): Behavior[AnalyticsClientCommand] = {
    Behaviors.setup { context =>

      implicit val system: ActorSystem[Nothing] = context.system

      val trackingId: String = system.settings.config
        .getString("googleAnalytics.trackingId")

      val clientId: String = system.settings.config
        .getString("googleAnalytics.clientId")

      Behaviors.receiveMessage[AnalyticsClientCommand] {

        case TrackSearch(rawParams, host, path, dplaDocList, searchType) =>

          // Track pageview
          // Strip the API key out of the page path
          val cleanParams = rawParams.filterNot(_._1 == "api_key")
          val query: String = paramString(cleanParams)
          val pathWithQuery: String = Seq(path, query).mkString("?")

          val pageViewParams: String = trackPageViewParams(
            trackingId,
            clientId,
            host,
            pathWithQuery,
            s"$searchType search results"
          )
          postHit(system, pageViewParams)

          // Track events
          if (dplaDocList.nonEmpty) {
            val eventParams: Seq[String] = dplaDocList.map(doc =>
              trackEventParams(
                trackingId,
                clientId,
                host,
                path,
                eventCategory(doc, searchType),
                eventAction(doc),
                eventLabel(doc)
              )
            )
            postBatch(system, eventParams)
          }

          Behaviors.same

        case TrackFetch(host, path, dplaDoc, searchType) =>

          // Track pageview
          val pageViewParams: String = trackPageViewParams(
            trackingId,
            clientId,
            host,
            path,
            s"Fetch $searchType"
          )
          postHit(system, pageViewParams)

          // Track event
          dplaDoc match {
            case Some(doc) =>
              val eventParams: String = trackEventParams(
                trackingId,
                clientId,
                host,
                path,
                eventCategory(doc, searchType),
                eventAction(doc),
                eventLabel(doc)
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
                                   clientId: String,
                                   host: String,
                                   path: String,
                                   title: String
                                 ): String = {
    val params = Map(
      "v" -> "1",
      "t" -> "pageview",
      "tid" -> trackingId,
      "cid" -> clientId,
      "dh" -> host,
      "dp" -> path,
      "dt" -> title
    )
    paramString(params)
  }

  private def trackEventParams(
                                trackingId: String,
                                clientId: String,
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
      "cid" -> clientId,
      "dh" -> host,
      "dp" -> path,
      "ec" -> category,
      "ea" -> action,
      "el" -> label
    )
    paramString(params)
  }

  private def eventCategory(doc: JsValue, searchType: String): String = {
    val root = doc.asJsObject
    val provider = readString(root, "provider", "name").getOrElse("")
    s"View API $searchType : $provider"
  }

  private def eventAction(doc: JsValue): String = {
    val root = doc.asJsObject
    readString(root, "dataProvider", "name").getOrElse("")
  }

  private def eventLabel(doc: JsValue): String = {
    val root = doc.asJsObject
    val docId = readString(root, "id").getOrElse("")
    val title = readStringArray(root, "sourceResource", "title").mkString(", ")
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
    params.map { case (key, value) => s"$key=$value" }.mkString("&")
}
