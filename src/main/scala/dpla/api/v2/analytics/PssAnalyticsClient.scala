package dpla.api.v2.analytics

import akka.actor.typed.ActorSystem

import dpla.api.v2.search.mappings.{MappedDocList, SingleMappedDoc}

/**
 * Tracks pageviews and events for items for primary source sets
 */
object PssAnalyticsClient extends AnalyticsClient {

  override protected def trackSearch(
                                      cleanParams: Map[String, String],
                                      host: String,
                                      path: String,
                                      mappedDocList: MappedDocList,
                                      system: ActorSystem[Nothing]): Unit = {

    // Track pageview
    val query: String = paramString(cleanParams)
    val pathWithQuery: String = Seq(path, query).mkString("?")
    val title = s"Primary Source Set search results"
    val pageViewParams = getPageViewParams(host, pathWithQuery, title, system)
    postHit(system, pageViewParams)
  }

  override protected def trackFetch(
                                     host: String,
                                     path: String,
                                     singleMappedDoc: SingleMappedDoc,
                                     system: ActorSystem[Nothing]): Unit = {

    // Track pageview
    val title = s"Fetch Primary Source Set"
    val pageViewParams = getPageViewParams(host, path, title, system)
    postHit(system, pageViewParams)
  }
}
