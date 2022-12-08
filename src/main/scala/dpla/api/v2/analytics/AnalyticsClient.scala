package dpla.api.v2.analytics

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import dpla.api.v2.search.mappings.{JsonFieldReader, MappedDocList, MappedResponse, SingleMappedDoc}

/**
 * Tracks use via Google Analytics Measurement Protocol
 *
 * @see https://developers.google.com/analytics/devguides/collection/protocol/v1
 *
 * Google Analytics Measurement Protocol does not return HTTP codes, so the
 * success or failure of a request cannot be ascertained.
 */

sealed trait AnalyticsClientCommand

final case class TrackSearch(
                              rawParams: Map[String, String],
                              host: String,
                              path: String,
                              mappedResponse: MappedResponse,
                            ) extends AnalyticsClientCommand

final case class TrackFetch(
                             host: String,
                             path: String,
                             singleMappedDoc: SingleMappedDoc
                           ) extends AnalyticsClientCommand

trait AnalyticsClient extends JsonFieldReader {

  val collectUrl = "https://www.google-analytics.com/collect"
  val batchUrl = "http://www.google-analytics.com/batch"


  /** Abstract methods */
  protected def trackSearch(rawParams: Map[String, String],
                            host: String,
                            path: String,
                            mappedResponse: MappedResponse,
                            system: ActorSystem[Nothing]): Unit

  protected def trackFetch(host: String,
                           path: String,
                           singleMappedDoc: SingleMappedDoc,
                           system: ActorSystem[Nothing]): Unit


  def apply(): Behavior[AnalyticsClientCommand] = {
    Behaviors.setup { context =>

      implicit val system: ActorSystem[Nothing] = context.system

      Behaviors.receiveMessage[AnalyticsClientCommand] {

        case TrackSearch(rawParams, host, path, mappedResponse) =>
          // Strip the API key out of the page path
          val cleanParams = rawParams.filterNot(_._1 == "api_key")
          trackSearch(cleanParams, host, path, mappedResponse, system)
          Behaviors.same

        case TrackFetch(host, path, singleMappedDoc) =>
          trackFetch(host, path, singleMappedDoc, system)
          Behaviors.same
      }
    }
  }

  protected def getTrackingId(system: ActorSystem[Nothing]): String =
    system.settings.config.getString("googleAnalytics.trackingId")

  protected def getClientId(system: ActorSystem[Nothing]): String =
    system.settings.config.getString("googleAnalytics.clientId")

  protected def getPageViewParams(
                                   host: String,
                                    path: String,
                                    title: String,
                                    system: ActorSystem[Nothing]
                                  ): String = {
    val params = Map(
      "v" -> "1",
      "t" -> "pageview",
      "tid" -> getTrackingId(system),
      "cid" -> getClientId(system),
      "dh" -> host,
      "dp" -> path,
      "dt" -> title
    )
    paramString(params)
  }

  protected def getEventParams(
                                host: String,
                                path: String,
                                category: String,
                                action: String,
                                label: String,
                                system: ActorSystem[Nothing]
                              ): String = {
    val params = Map(
      "v" -> "1",
      "t" -> "event",
      "tid" -> getTrackingId(system),
      "cid" -> getClientId(system),
      "dh" -> host,
      "dp" -> path,
      "ec" -> category,
      "ea" -> action,
      "el" -> label
    )
    paramString(params)
  }

  protected def postHit(
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

  protected def postBatch(
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
  protected def paramString(params: Map[String, String]): String =
    params.map { case (key, value) => s"$key=$value" }.mkString("&")
}
