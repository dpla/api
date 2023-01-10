package dpla.api.v2.search.paramValidators

import dpla.api.v2.search.models.DPLAMAPFields

object ItemParamValidator extends ParamValidator with DPLAMAPFields {

  // These parameters are valid for a search request.
  override protected val acceptedSearchParams: Seq[String] =
    searchableDataFields ++ Seq(
      "exact_field_match",
      "facets",
      "facet_size",
      "fields",
      "filter",
      "op",
      "page",
      "page_size",
      "q",
      "sort_by",
      "sort_by_pin",
      "sort_order"
    )

  // These parameters are valid for a fetch request.
  override protected val acceptedFetchParams: Seq[String] = Seq()

  // These fields are not valid for search, sort, filter, & facets.
  // Rather than returning an error, they should be ignored.
  override protected val ignoredFields: Seq[String] = Seq(
    "sourceResource.subtitle"
  )

  // No pre-processing necessary.
  override protected def preProcess(unprocessed: Map[String, String]): Map[String, String] =
    unprocessed
}
