package dpla.api.v2.search

/**
 * Holds information about Item DPLA MAP fields and their equivalents in
 * ElasticSearch.
 * Fields must be defined in order to make them searchable, facetable,
 * sortable, or usable in a "fields=..." query.
 * Fields will appear in the search/fetch results doc body whether or not they
 * are defined here.
 */
trait DPLAMAPFields extends FieldDefinitions {

  override val fields = Seq(
    DplaField(
      name = "@context",
      fieldType = DisabledField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "@context",
      elasticSearchNotAnalyzed = None
    ),
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
      name = "admin.contributingInstitution",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = false,
      elasticSearchDefault = "admin.contributingInstitution",
      elasticSearchNotAnalyzed = Some("admin.contributingInstitution")
    ),
    DplaField(
      name = "aggregatedCHO",
      fieldType = DisabledField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "aggregatedCHO",
      elasticSearchNotAnalyzed = None
    ),
    DplaField(
      name = "dataProvider",
      fieldType = WildcardField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "dataProvider.*",
      elasticSearchNotAnalyzed = Some("dataProvider.name.not_analyzed")
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
      name = "hasView.edmRights",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "hasView.edmRights",
      elasticSearchNotAnalyzed = Some("hasView.edmRights.not_analyzed")
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
      name = "id",
      fieldType = TextField,
      searchable = true,
      facetable = false,
      sortable = true,
      elasticSearchDefault = "id",
      elasticSearchNotAnalyzed = Some("id")
    ),
    DplaField(
      name = "iiifManifest",
      fieldType = URLField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "iiifManifest",
      elasticSearchNotAnalyzed = Some("iiifManifest")
    ),
    DplaField(
      name = "ingestDate",
      fieldType = DisabledField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "ingestDate",
      elasticSearchNotAnalyzed = None
    ),
    DplaField(
      name = "ingestType",
      fieldType = DisabledField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "ingestType",
      elasticSearchNotAnalyzed = None
    ),
    DplaField(
      name = "ingestSequence",
      fieldType = DisabledField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "ingestSequence",
      elasticSearchNotAnalyzed = None
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
      name = "mediaMaster",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "mediaMaster",
      elasticSearchNotAnalyzed = Some("mediaMaster")
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
      name = "originalRecord",
      fieldType = DisabledField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "originalRecord",
      elasticSearchNotAnalyzed = None
    ),
    DplaField(
      name = "provider",
      fieldType = WildcardField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "provider.*",
      elasticSearchNotAnalyzed = None
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
      name = "sourceResource.@id",
      fieldType = DisabledField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "sourceResource.@id",
      elasticSearchNotAnalyzed = None
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
      name = "sourceResource.date.begin",
      fieldType = DateField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.date.begin",
      elasticSearchNotAnalyzed = Some("sourceResource.date.begin.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.date.displayDate",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "sourceResource.date.displayDate",
      elasticSearchNotAnalyzed = None
    ),
    DplaField(
      name = "sourceResource.date.end",
      fieldType = DateField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.date.end",
      elasticSearchNotAnalyzed = Some("sourceResource.date.end.not_analyzed")
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
      elasticSearchNotAnalyzed = Some("sourceResource.format")
    ),
    DplaField(
      name = "sourceResource.genre",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "sourceResource.genre",
      elasticSearchNotAnalyzed = Some("sourceResource.genre")
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
      name = "isPartOf",
      fieldType = DisabledField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "isPartOf",
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
      name = "sourceResource.spatial",
      fieldType = WildcardField,
      searchable = true,
      facetable= false,
      sortable = false,
      elasticSearchDefault = "sourceResource.spatial.*",
      elasticSearchNotAnalyzed = None
    ),
    DplaField(
      name = "sourceResource.spatial.city",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.spatial.city",
      elasticSearchNotAnalyzed = Some("sourceResource.spatial.city.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.spatial.coordinates",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.spatial.coordinates",
      elasticSearchNotAnalyzed = Some("sourceResource.spatial.coordinates")
    ),
    DplaField(
      name = "sourceResource.spatial.country",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.spatial.country",
      elasticSearchNotAnalyzed = Some("sourceResource.spatial.country.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.spatial.county",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.spatial.county",
      elasticSearchNotAnalyzed = Some("sourceResource.spatial.county.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.spatial.iso3166-2",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "sourceResource.spatial.iso3166-2",
      elasticSearchNotAnalyzed = Some("sourceResource.spatial.iso3166-2")
    ),
    DplaField(
      name = "sourceResource.spatial.name",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.spatial.name",
      elasticSearchNotAnalyzed = Some("sourceResource.spatial.name.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.spatial.region",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.spatial.region",
      elasticSearchNotAnalyzed = Some("sourceResource.spatial.region.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.spatial.state",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.spatial.state",
      elasticSearchNotAnalyzed = Some("sourceResource.spatial.state.not_analyzed")
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
      name = "sourceResource.stateLocatedIn",
      fieldType = DisabledField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "sourceResource.stateLocatedIn",
      elasticSearchNotAnalyzed = None
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
      name = "sourceResource.subject.@type",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "sourceResource.subject.@type",
      elasticSearchNotAnalyzed = Some("sourceResource.subject.@type")
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
      name = "sourceResource.subject.scheme",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "sourceResource.subject.scheme",
      elasticSearchNotAnalyzed = Some("sourceResource.subject.scheme.not_analyzed")
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
      name = "sourceResource.temporal.after",
      fieldType = DateField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "sourceResource.temporal.begin",
      elasticSearchNotAnalyzed = Some("sourceResource.temporal.begin.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.temporal.before",
      fieldType = DateField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "sourceResource.temporal.end",
      elasticSearchNotAnalyzed = Some("sourceResource.temporal.end.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.temporal.begin",
      fieldType = DateField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.temporal.begin",
      elasticSearchNotAnalyzed = Some("sourceResource.temporal.begin.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.temporal.end",
      fieldType = DateField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceResource.temporal.end",
      elasticSearchNotAnalyzed = Some("sourceResource.temporal.end.not_analyzed")
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

  override val coordinatesField: Option[DplaField] =
    fields.find(_.name == "sourceResource.spatial.coordinates")

  override val dateFields: Seq[DplaField] =
    fields.filter(_.fieldType == DateField)
}
