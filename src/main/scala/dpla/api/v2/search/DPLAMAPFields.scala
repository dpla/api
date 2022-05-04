package dpla.api.v2.search

/**
 * Holds information about Item DPLA MAP fields and their equivalents in
 * ElasticSearch.
 * This list does not need to include all fields, only those that are
 * searchable, facetable, filterable, or sortable.
 * Fields will be mapped whether or not they are included in this list.
 */
trait DPLAMAPFields extends FieldDefinitions {

  override val fields = Seq(
    DplaField(
      name = "@id",
      fieldType = URLField,
      searchable = false,
      facetable = false,
      sortable = true,
      elasticSearchDefault = "@id",
      elasticSearchNotAnalyzed = Some("@id")
    ),
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
      searchable = true,
      facetable = true,
      sortable = false,
      elasticSearchDefault = "admin.contributingInstitution",
      elasticSearchNotAnalyzed = Some("admin.contributingInstitution")
    ),
    DplaField(
      name = "dataProvider.@id",
      fieldType = URLField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "dataProvider.@id",
      elasticSearchNotAnalyzed = Some("dataProvider.@id")
    ),
    DplaField(
      name = "dataProvider.exactMatch",
      fieldType = URLField,
      searchable = true,
      facetable = true,
      sortable = false,
      elasticSearchDefault = "dataProvider.exactMatch",
      elasticSearchNotAnalyzed = Some("dataProvider.exactMatch")
    ),
    DplaField(
      name = "dataProvider.name",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "dataProvider.name",
      elasticSearchNotAnalyzed = Some("dataProvider.name.not_analyzed")
    ),
    DplaField(
      name = "hasView.@id",
      fieldType = URLField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "hasView.@id",
      elasticSearchNotAnalyzed = Some("hasView.@id")
    ),
    DplaField(
      name = "hasView.format",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "hasView.format",
      elasticSearchNotAnalyzed = Some("hasView.format")
    ),
    DplaField(
      name = "hasView.rights",
      fieldType = TextField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "hasView.rights",
      elasticSearchNotAnalyzed = Some("hasView.rights")
    ),
    DplaField(
      name = "intermediateProvider",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = false,
      elasticSearchDefault = "intermediateProvider",
      elasticSearchNotAnalyzed = Some("intermediateProvider.not_analyzed")
    ),
    DplaField(
      name = "isPartOf.@id",
      fieldType = URLField,
      searchable = false,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "isPartOf.@id",
      elasticSearchNotAnalyzed = Some("isPartOf.@id")
    ),
    DplaField(
      name = "isPartOf.name",
      fieldType = TextField,
      searchable = false,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "isPartOf.name",
      elasticSearchNotAnalyzed = Some("isPartOf.name.not_analyzed")
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
      name = "provider.exactMatch",
      fieldType = URLField,
      searchable = true,
      facetable = true,
      sortable = false,
      elasticSearchDefault = "provider.exactMatch",
      elasticSearchNotAnalyzed = Some("provider.exactMatch")
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
      name = "rights",
      fieldType = URLField,
      searchable = true,
      facetable = true,
      sortable = false,
      elasticSearchDefault = "rights",
      elasticSearchNotAnalyzed = Some("rights")
    ),
    DplaField(
      name = "rightsCategory",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = false,
      elasticSearchDefault = "rightsCategory",
      elasticSearchNotAnalyzed = Some("rightsCategory")
    ),
    DplaField(
      name = "tags",
      fieldType = TextField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "tags",
      elasticSearchNotAnalyzed = Some("tags")
    ),
    DplaField(
      name = "sourceResource.collection.@id",
      fieldType = URLField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "sourceResource.collection.@id",
      elasticSearchNotAnalyzed = Some("sourceResource.collection.@id")
    ),
    DplaField(
      name = "sourceResource.collection.id",
      fieldType = TextField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "sourceResource.collection.id",
      elasticSearchNotAnalyzed = Some("sourceResource.collection.id")
    ),
    DplaField(
      name = "sourceResource.collection.description",
      fieldType = TextField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "sourceResource.collection.description",
      elasticSearchNotAnalyzed = None
    ),
    DplaField(
      name = "sourceResource.collection.title",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = false,
      elasticSearchDefault = "sourceResource.collection.title",
      elasticSearchNotAnalyzed = Some("sourceResource.collection.title.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.contributor",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.contributor",
      elasticSearchNotAnalyzed = Some("sourceResource.contributor")
    ),
    DplaField(
      name = "sourceResource.creator",
      fieldType = TextField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "sourceResource.creator",
      elasticSearchNotAnalyzed = None
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
      name = "sourceResource.extent",
      fieldType = TextField,
      searchable = true,
      facetable = false,
      sortable = true,
      elasticSearchDefault = "sourceResource.extent",
      elasticSearchNotAnalyzed = Some("sourceResource.extent")
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
      name = "sourceResource.identifier",
      fieldType = TextField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "sourceResource.identifier",
      elasticSearchNotAnalyzed = None
    ),
    DplaField(
      name = "sourceResource.language.iso639_3",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.language.iso639_3",
      elasticSearchNotAnalyzed = Some("sourceResource.language.iso639_3"),
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
      name = "sourceResource.relation",
      fieldType = TextField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "sourceResource.relation",
      elasticSearchNotAnalyzed = None
    ),
    DplaField(
      name = "sourceResource.rights",
      fieldType = TextField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "sourceResource.rights",
      elasticSearchNotAnalyzed = None
    ),
    DplaField(
      name = "sourceResource.specType",
      fieldType = TextField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "sourceResource.relation",
      elasticSearchNotAnalyzed = Some("sourceResource.specType")
    ),
    DplaField(
      name = "sourceResource.subject.@id",
      fieldType = URLField,
      searchable = false,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.subject.@id",
      elasticSearchNotAnalyzed = Some("sourceResource.subject.@id")
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
    ),
    DplaField(
      name = "sourceResource.type",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.type",
      elasticSearchNotAnalyzed = Some("sourceResource.type")
    )
  )
}
