package dpla.api.v2.search

/**
 * Holds information about Ebook DPLA MAP fields and their equivalents in
 * ElasticSearch.
 */
trait EbookFields extends FieldDefinitions {

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
      name = "isShownAt",
      fieldType = URLField,
      searchable = true,
      facetable = false,
      sortable = true,
      elasticSearchDefault = "itemUri",
      elasticSearchNotAnalyzed = Some("itemUri")
    ),
    DplaField(
      name = "object",
      fieldType = URLField,
      searchable = true,
      facetable = false,
      sortable = true,
      elasticSearchDefault = "payloadUri",
      elasticSearchNotAnalyzed = Some("payloadUri")
    ),
    DplaField(
      name = "provider.@id",
      fieldType = URLField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "sourceUri",
      elasticSearchNotAnalyzed = Some("sourceUri")
    ),
    DplaField(
      name = "provider.name",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "providerName",
      elasticSearchNotAnalyzed = Some("providerName.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.creator",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "author",
      elasticSearchNotAnalyzed = Some("author.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.date.displayDate",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = false,
      elasticSearchDefault = "publicationDate",
      elasticSearchNotAnalyzed = Some("publicationDate.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.description",
      fieldType = TextField,
      searchable = true,
      facetable = false,
      sortable = false,
      elasticSearchDefault = "summary",
      elasticSearchNotAnalyzed = None
    ),
    DplaField(
      name = "sourceResource.format",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "medium",
      elasticSearchNotAnalyzed = Some("medium.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.language.name",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "language",
      elasticSearchNotAnalyzed = Some("language.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.publisher",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "publisher",
      elasticSearchNotAnalyzed = Some("publisher.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.subject.name",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "genre",
      elasticSearchNotAnalyzed = Some("genre.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.subtitle",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "subtitle",
      elasticSearchNotAnalyzed = Some("subtitle.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.title",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      sortable = true,
      elasticSearchDefault = "title",
      elasticSearchNotAnalyzed = Some("title.not_analyzed")
    )
  )
}
