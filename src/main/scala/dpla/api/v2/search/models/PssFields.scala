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
      name = "hasPart.@id",
      fieldType = TextField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "hasPart.@id",
      elasticSearchNotAnalyzed = None
    )
  )

  override val coordinatesField: Option[DataField] = None

  override val dateFields: Seq[DataField] = Seq()
}
