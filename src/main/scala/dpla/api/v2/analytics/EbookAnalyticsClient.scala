package dpla.api.v2.analytics

/**
 * Tracks use for Ebooks
 */
object EbookAnalyticsClient extends DPLAMAPAnalyticsClient {
  override protected val docType: String = "Ebook"
}
