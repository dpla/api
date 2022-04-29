package dpla.api.v2.search

object EbookParamValidator extends ParamValidator with EbookFields {

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

  // These fields are valid for DPLA item search & facets, but not for ebooks.
  // Rather than returning an error, they should be ignored.
  override protected val ignoredFields: Seq[String] = Seq(
    "admin.contributingInstitution",
    "rightsCategory",
    "sourceResource.collection.title",
    "sourceResource.date.begin",
    "sourceResource.date.end",
    "sourceResource.spatial.name",
    "sourceResource.type"
  )
}
