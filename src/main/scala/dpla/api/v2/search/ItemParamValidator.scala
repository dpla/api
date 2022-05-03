package dpla.api.v2.search

object ItemParamValidator extends ParamValidator with DPLAMAPFields {

  // A user can give any of the following parameters in a search request.
  override protected val acceptedSearchParams: Seq[String] =
    searchableDplaFields ++ Seq(
      "exact_field_match",
      "facets",
      "facet_size",
      "fields",
      "op",
      "page",
      "page_size",
      "q",
      "sort_by",
      "sort_order"
    )

  // These fields are valid for DPLA ebook search & facets, but not for items.
  // Rather than returning an error, they should be ignored.
  override protected val ignoredFields: Seq[String] = Seq(
    "sourceResource.subtitle"
  )
}
