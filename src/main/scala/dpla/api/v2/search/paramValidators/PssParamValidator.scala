package dpla.api.v2.search.paramValidators

import dpla.api.v2.search.models.PssFields

object PssParamValidator extends ParamValidator with PssFields {

  // These parameters are valid for a search request.
  override protected val acceptedSearchParams: Seq[String] =
    searchableDataFields ++ Seq("fields")

  // These parameters are valid for a fetch request.
  override protected val acceptedFetchParams: Seq[String] = Seq()

  // These fields are not valid for search, sort, filter, & facets.
  // Rather than returning an error, they should be ignored.
  override protected val ignoredFields: Seq[String] = Seq()

  // Return all primary source sets by default.
  override protected val defaultPageSize: Int = 200

  override protected def preProcess(unprocessed: Map[String, String]): Map[String, String] = {
    if (unprocessed.keys.toSet.contains("@id")) {
      unprocessed
    } else if (unprocessed.keys.toSet.contains("source_slug")) {
      // Get a source
      unprocessed
    } else {
      // Get all sets
      unprocessed + ("fields" -> allSetsFields.mkString(","))
    }
  }

  val allSetsFields: Seq[String] = Seq(
    "@context",
    "@id",
    "@type",
    "about",
    "name",
    "repImageUrl",
    "thumbnailUrl"
  )
}
