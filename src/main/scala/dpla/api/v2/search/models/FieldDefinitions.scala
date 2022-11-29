package dpla.api.v2.search.models

trait FieldDefinitions {

  protected sealed trait DataFieldType

  case object TextField extends DataFieldType

  case object URLField extends DataFieldType

  case object DateField extends DataFieldType

  case object DisabledField extends DataFieldType

  case object WildcardField extends DataFieldType

  /**
   * @param name                     Data field name
   * @param fieldType                One of DataFieldType
   * @param searchable               Can users keyword search within this field?
   * @param facetable                Can users facet on this field?
   *                                 Must have elasticSearchNotAnalyzed.
   * @param sortable                 Can users sort on this field?
   *                                 Must have elasticSearchNotAnalyzed.
   * @param elasticSearchDefault     ElasticSearch field name.
   *                                 Can be either analyzed or not analyzed.
   * @param elasticSearchNotAnalyzed ElasticSearch field name, not analyzed.
   */
  case class DataField(
                        name: String,
                        fieldType: DataFieldType,
                        searchable: Boolean,
                        facetable: Boolean,
                        sortable: Boolean,
                        elasticSearchDefault: String,
                        elasticSearchNotAnalyzed: Option[String] = None
                      )

  // Abstract
  val fields: Seq[DataField]
  val coordinatesField: Option[DataField]
  val dateFields: Seq[DataField]

  def allDataFields: Seq[String] =
    fields.map(_.name)

  def searchableDataFields: Seq[String] =
    fields.filter(_.searchable).map(_.name)

  def facetableDataFields: Seq[String] =
    fields
      .filter(_.facetable)
      .filter(_.elasticSearchNotAnalyzed.nonEmpty)
      .map(_.name)

  def sortableDataFields: Seq[String] =
    fields
      .filter(_.sortable)
      .filter(_.elasticSearchNotAnalyzed.nonEmpty)
      .map(_.name)

  def getElasticSearchField(name: String): Option[String] =
    fields.find(_.name == name).map(_.elasticSearchDefault)

  def getElasticSearchNotAnalyzed(name: String): Option[String] =
    fields.find(_.name == name).flatMap(_.elasticSearchNotAnalyzed)

  /**
   * Map data field to ElasticSearch non-analyzed field.
   * If a field is only indexed as analyzed (text), then return analyzed field.
   * Used for exact field matches and facets.
   */
  def getElasticSearchExactMatchField(name: String): Option[String] =
    fields.find(_.name == name)
      .map(field =>
        field.elasticSearchNotAnalyzed.getOrElse(field.elasticSearchDefault)
      )

  def getDataFieldType(name: String): Option[DataFieldType] =
    fields.find(_.name == name).map(_.fieldType)
}
