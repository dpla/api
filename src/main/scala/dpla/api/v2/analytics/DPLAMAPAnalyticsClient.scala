package dpla.api.v2.analytics

import akka.actor.typed.ActorSystem
import dpla.api.v2.search.mappings.{DPLADocList, MappedResponse, SingleDPLADoc}
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

    mappedResponse match {
      case dplaDocList: DPLADocList =>
        trackMultiDoc(cleanParams, host, path, dplaDocList, system)
      case singleDPLADoc: SingleDPLADoc =>
        trackSingleDoc(host, path, singleDPLADoc, system)
      case _ => // no-op
    }
  }

  private def trackMultiDoc(
                     cleanParams: Map[String, String],
                     host: String,
                     path: String,
                     dplaDocList: DPLADocList,
                     system: ActorSystem[Nothing]): Unit = {

    // Track pageview
    val query: String = paramString(cleanParams)
    val pathWithQuery: String = Seq(path, query).mkString("?")
    val title = s"$docType search results"
    val pageViewParams = getPageViewParams(host, pathWithQuery, title, system)
    postHit(system, pageViewParams)

    // Track events
    if (dplaDocList.docs.nonEmpty) {

      val eventParams: Seq[String] = dplaDocList.docs.map(doc =>
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

  private def trackSingleDoc(
                      host: String,
                      path: String,
                      singleDPLADoc: SingleDPLADoc,
                      system: ActorSystem[Nothing]): Unit = {

    // Track pageview
    val title = s"Fetch $docType"
    val pageViewParams = getPageViewParams(host, path, title, system)
    postHit(system, pageViewParams)

    // Track event
    val dplaDoc: Option[JsValue] = singleDPLADoc.docs.headOption

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
