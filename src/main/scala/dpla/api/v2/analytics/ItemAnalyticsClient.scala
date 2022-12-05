package dpla.api.v2.analytics

/**
 * Tracks use for Items
 */
object ItemAnalyticsClient extends DPLAMAPAnalyticsClient {
  override protected val docType: String = "Item"
}
