package dpla.ebookapi.v1.ebooks

/**
 * Holds information about DPLA MAP fields.
 */
object DplaMapFields {

  sealed trait DplaFieldType
  case object TextField extends DplaFieldType
  case object URLField extends DplaFieldType

  case class DplaField(
                        name: String,
                        fieldType: DplaFieldType,
                        searchable: Boolean, // Can users do a keyword search within this field?
                        facetable: Boolean, // Can users facet on this field? Must have elasticSearchNotAnalyzed.
                        elasticSearchDefault: String, // Can be either analyzed or not analyzed
                        elasticSearchNotAnalyzed: Option[String] = None
                     )

  private final val fields = Seq(
    DplaField(
      name = "isShownAt",
      fieldType = URLField,
      searchable = true,
      facetable = false,
      elasticSearchDefault = "itemUri",
      elasticSearchNotAnalyzed = Some("itemUri")
    ),
    DplaField(
      name = "object",
      fieldType = URLField,
      searchable = true,
      facetable = false,
      elasticSearchDefault = "payloadUri",
      elasticSearchNotAnalyzed = Some("payloadUri")
    ),
    DplaField(
      name = "provider.@id",
      fieldType = URLField,
      searchable = true,
      facetable = true,
      elasticSearchDefault = "sourceUri",
      elasticSearchNotAnalyzed = Some("sourceUri")
    ),
    DplaField(
      name = "sourceResource.creator",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      elasticSearchDefault = "author",
      elasticSearchNotAnalyzed = Some("author.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.date.displayDate",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      elasticSearchDefault = "publicationDate",
      elasticSearchNotAnalyzed = Some("publicationDate.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.description",
      fieldType = TextField,
      searchable = true,
      facetable = false,
      elasticSearchDefault = "summary",
      elasticSearchNotAnalyzed = None
    ),
    DplaField(
      name = "sourceResource.format",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      elasticSearchDefault = "medium",
      elasticSearchNotAnalyzed = Some("medium.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.language.name",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      elasticSearchDefault = "language",
      elasticSearchNotAnalyzed = Some("language.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.publisher",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      elasticSearchDefault = "publisher",
      elasticSearchNotAnalyzed = Some("publisher.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.subject.name",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      elasticSearchDefault = "genre",
      elasticSearchNotAnalyzed = Some("genre.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.subtitle",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      elasticSearchDefault = "subtitle",
      elasticSearchNotAnalyzed = Some("subtitle.not_analyzed")
    ),
    DplaField(
      name = "sourceResource.title",
      fieldType = TextField,
      searchable = true,
      facetable = true,
      elasticSearchDefault = "title",
      elasticSearchNotAnalyzed = Some("title.not_analyzed")
    )
  )

  def allFields: Seq[String] =
    fields.map(_.name)

  def searchableFields: Seq[String] =
    fields.filter(_.searchable).map(_.name)

  def facetableFields: Seq[String] =
    fields.filter(_.facetable).filter(_.elasticSearchNotAnalyzed.nonEmpty).map(_.name)

  def getElasticSearchField(name: String): Option[String] =
    fields.find(_.name == name).map(_.elasticSearchDefault)

  /**
   * Map DPLA MAP field to ElasticSearch non-analyzed field.
   * If a field is only indexed as analyzed (text), then return the analyzed field.
   * Used for exact field matches and facets.
   */
  def getElasticSearchExactMatchField(name: String): Option[String] =
    fields.find(_.name == name).map(field => field.elasticSearchNotAnalyzed.getOrElse(field.elasticSearchDefault))

  def getFieldType(name: String): Option[DplaFieldType] =
    fields.find(_.name == name).map(_.fieldType)
}
