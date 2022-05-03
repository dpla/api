package dpla.api.v2.search

/**
 * Holds information about Item DPLA MAP fields and their equivalents in
 * ElasticSearch.
 */
trait DPLAMAPFields extends FieldDefinitions {

  override val fields = Seq(
    DplaField(
      name = "id",
      fieldType = TextField,
      searchable = true,
      facetable = false,
      sortable = true,
      elasticSearchDefault = "id",
      elasticSearchNotAnalyzed = Some("id")
    ),
    DplaField(
      name = "admin.contributingInstitution",
      fieldType = TextField,
      searchable = ???,
      facetable = ???,
      sortable = ???,
      elasticSearchDefault = "admin.contributingInstitution",
      elasticSearchNotAnalyzed = Some("admin.contributingInstitution")
    ),
    DplaField(
      name = "isShownAt",
      fieldType = URLField,
      searchable = true,
      facetable = false,
      sortable = true,
      elasticSearchDefault = "isShownAt",
      elasticSearchNotAnalyzed = Some("isShownAt")
    ),
    DplaField(
      name = "object",
      fieldType = URLField,
      searchable = true,
      facetable = false,
      sortable = true,
      elasticSearchDefault = "object",
      elasticSearchNotAnalyzed = Some("object")
    ),
    DplaField(
      name = "provider.@id",
      fieldType = URLField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "provider.@id",
      elasticSearchNotAnalyzed = Some("provider.@id")
    ),
    DplaField(
      name = "provider.name",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "provider.name",
      elasticSearchNotAnalyzed = Some("provider.name.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.creator",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.creator",
      elasticSearchNotAnalyzed = Some("sourceResource.creator.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.date.displayDate",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = false,
      elasticSearchDefault = "sourceResource.date.displayDate",
      elasticSearchNotAnalyzed = Some("sourceResource.date.displayDate.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.description",
      fieldType = TextField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "sourceResource.description",
      elasticSearchNotAnalyzed = None
    ),
    DplaField(
      name = "sourceResource.format",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.format",
      elasticSearchNotAnalyzed = Some("sourceResource.format.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.language.name",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.language.name",
      elasticSearchNotAnalyzed = Some("sourceResource.language.name.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.publisher",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.publisher",
      elasticSearchNotAnalyzed = Some("sourceResource.publisher.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.subject.name",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.subject.name",
      elasticSearchNotAnalyzed = Some("sourceResource.subject.name.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.subtitle",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.subtitle",
      elasticSearchNotAnalyzed = Some("sourceResource.subtitle.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.title",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.title",
      elasticSearchNotAnalyzed = Some("sourceResource.title.not_analyzed")
    )
  )
}
