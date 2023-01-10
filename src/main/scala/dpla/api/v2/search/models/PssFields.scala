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
      name = "accessibilityControl",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "accessibilityControl",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "accessibilityFeature",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "accessbilityFeature", // typo in source data
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "accessibilityHazard",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "accessibilityHazard",
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
      name = "about.sameAs",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = true,
      elasticSearchDefault = "about.sameAs",
      elasticSearchNotAnalyzed = Some("about.sameAs")
    ),
    DataField(
      name = "author",
      fieldType = WildcardField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "author.*",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "dct:created",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "dct:created",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "dct:modified",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "dct:modified",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "dct:type",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "dct:type",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "dateCreated",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = true,
      elasticSearchDefault = "dateCreated",
      elasticSearchNotAnalyzed = Some("dateCreated")
    ),
    DataField(
      name = "dateModified",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "dateModified",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "description",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "description",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "educationalAlignment",
      fieldType = WildcardField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "educationalAlignment.*",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "hasPart",
      fieldType = WildcardField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "hasPart.*",
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
      name = "hasPart.@type",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "hasPart.@type",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "hasPart.disambiguatingDescription",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "hasPart.disambiguatingDescription",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "hasPart.id",
      fieldType = IntField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "hasPart.@id",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "hasPart.mainEntity.@type",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "hasPart.mainEntity.@type",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "hasPart.name",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "hasPart.name",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "hasPart.repImageUrl",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "hasPart.repImageUrl",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "hasPart.thumbnailUrl",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "hasPart.thumbnailUrl",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "hasPart.text",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "hasPart.text",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "id",
      fieldType = TextField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "@id",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "inLanguage",
      fieldType = WildcardField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "inLanguage.*",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "isRelatedTo",
      fieldType = WildcardField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "isRelatedTo.*",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "learningResourceType",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "learningResourceType",
      elasticSearchNotAnalyzed = None
    ),
    DataField(
      name = "license",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "license",
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
      name = "publisher",
      fieldType = WildcardField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "publisher.*",
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
    DataField(
      name = "typicalAgeRange",
      fieldType = TextField,
      searchable = false,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "typicalAgeRange",
      elasticSearchNotAnalyzed = None
    )
  )

  override val coordinatesField: Option[DataField] = None

  override val dateFields: Seq[DataField] = Seq()
}
