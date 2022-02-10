package dpla.ebookapi.v1.ebooks

/**
 * Holds information about DPLA MAP fields.
 */
trait DplaMapFields {

  sealed trait DplaFieldType
  case object TextField extends DplaFieldType
  case object URLField extends DplaFieldType

  /**
   * @param name                      DPLA MAP field name
   * @param fieldType                 One of DplaFieldType
   * @param searchable                Can users keyword search within this field?
   * @param facetable                 Can users facet on this field?
   *                                  Must have elasticSearchNotAnalyzed.
   * @param sortable                  Can users sort on this field?
   * @param elasticSearchDefault      ElasticSearch field name.
   *                                  Can be either analyzed or not analyzed.
   * @param elasticSearchNotAnalyzed  ElasticSearch field name, not analyzed.
   */
  case class DplaField(
                        name: String,
                        fieldType: DplaFieldType,
                        searchable: Boolean,
                        facetable: Boolean,
                        sortable: Boolean,
                        elasticSearchDefault: String,
                        elasticSearchNotAnalyzed: Option[String] = None
                     )

  private final val fields = Seq(
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

  def allDplaFields: Seq[String] =
    fields.map(_.name)

  def searchableDplaFields: Seq[String] =
    fields.filter(_.searchable).map(_.name)

  def facetableDplaFields: Seq[String] =
    fields.filter(_.facetable).filter(_.elasticSearchNotAnalyzed.nonEmpty).map(_.name)

  def sortableDplaFields: Seq[String] =
    fields.filter(_.sortable).map(_.name)

  def getElasticSearchField(name: String): Option[String] =
    fields.find(_.name == name).map(_.elasticSearchDefault)

  /**
   * Map DPLA MAP field to ElasticSearch non-analyzed field.
   * If a field is only indexed as analyzed (text), then return the analyzed field.
   * Used for exact field matches and facets.
   */
  def getElasticSearchExactMatchField(name: String): Option[String] =
    fields.find(_.name == name).map(field => field.elasticSearchNotAnalyzed.getOrElse(field.elasticSearchDefault))

  def getDplaFieldType(name: String): Option[DplaFieldType] =
    fields.find(_.name == name).map(_.fieldType)
}
