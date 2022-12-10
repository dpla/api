package dpla.api.v2.search.models

/**
 * Holds information about Primary Source Sets fields and their equivalents in
 * ElasticSearch.
 * Fields must be defined in order to make them searchable, facetable,
 * sortable, or usable in a "fields=..." query.
 * Fields will appear in the search/fetch results doc body whether or not they
 * are defined here.
 */
trait PssFields extends FieldDefinitions {

  override val fields = Seq(
    DataField(
      name = "@context",
      fieldType = WildcardField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "@context.*",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "@id",
      fieldType = TextField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "@id",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "@type",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "@type",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "about",
      fieldType = WildcardField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "about.*",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "hasPart.@id",
      fieldType = TextField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "hasPart.@id",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "name",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "name",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "repImageUrl",
      fieldType = URLField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "repImageUrl",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "thumbnailUrl",
      fieldType = URLField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "thumbnailUrl",
      elasticSearchNotAnalyzed = None
    ),
  )

  override val coordinatesField: Option[DataField] = None

  override val dateFields: Seq[DataField] = Seq()
}
