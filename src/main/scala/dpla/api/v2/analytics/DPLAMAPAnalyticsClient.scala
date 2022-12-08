package dpla.api.v2.analytics

import akka.actor.typed.ActorSystem
import dpla.api.v2.search.mappings.{DPLADocList, MappedDocList, MappedResponse, SingleDPLADoc, SingleMappedDoc}
import spray.json.JsValue

/**
 * Tracks pageviews and events for docs in DPLAMAP format
 */
trait DPLAMAPAnalyticsClient extends AnalyticsClient {

  /** Abstract methods */
  protected val docType: String

  override protected def trackSearch(
                                      cleanParams: Map[String, String],
                                      host: String,
                                      path: String,
                                      mappedResponse: MappedResponse,
                                      system: ActorSystem[Nothing]): Unit = {

    // Track pageview
    val query: String = paramString(cleanParams)
    val pathWithQuery: String = Seq(path, query).mkString("?")
    val title = s"$docType search results"
    val pageViewParams = getPageViewParams(host, pathWithQuery, title, system)
    postHit(system, pageViewParams)

    // Track events
    val dplaDocList: Seq[JsValue] =
      mappedResponse match {
        case list: DPLADocList => list.docs
        case _ => Seq()
      }

    if (dplaDocList.nonEmpty) {
      val eventParams: Seq[String] = dplaDocList.map(doc =>
        getEventParams(
          host,
          path,
          eventCategory(doc, docType),
          eventAction(doc),
          eventLabel(doc),
          system
        )
      )
      postBatch(system, eventParams)
    }
  }

  override protected def trackFetch(
                                     host: String,
                                     path: String,
                                     singleMappedDoc: SingleMappedDoc,
                                     system: ActorSystem[Nothing]): Unit = {

    // Track pageview
    val title = s"Fetch $docType"
    val pageViewParams = getPageViewParams(host, path, title, system)
    postHit(system, pageViewParams)

    // Track event
    val dplaDoc: Option[JsValue] = singleMappedDoc match {
      case doc: SingleDPLADoc => doc.docs.headOption
      case _ => None
    }

    dplaDoc match {
      case Some(doc) =>
        val eventParams = getEventParams(
          host,
          path,
          eventCategory(doc, docType),
          eventAction(doc),
          eventLabel(doc),
          system
        )
        postHit(system, eventParams)
      case None => // no-op
    }
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
}
