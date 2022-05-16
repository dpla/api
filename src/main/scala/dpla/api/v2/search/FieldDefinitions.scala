package dpla.api.v2.search

trait FieldDefinitions {

  protected sealed trait DplaFieldType
  case object TextField extends DplaFieldType
  case object URLField extends DplaFieldType
  case object DisabledField extends DplaFieldType

  /**
   * @param name                      DPLA MAP field name
   * @param fieldType                 One of DplaFieldType
   * @param searchable                Can users keyword search within this field?
   * @param facetable                 Can users facet on this field?
   *                                  Must have elasticSearchNotAnalyzed.
   * @param sortable                  Can users sort on this field?
   *                                  Must have elasticSearchNotAnalyzed.
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

  // Abstract
  val fields: Seq[DplaField]
  val coordinatesField: Option[DplaField]

  def allDplaFields: Seq[String] =
    fields.map(_.name)

  def searchableDplaFields: Seq[String] =
    fields.filter(_.searchable).map(_.name)

  def facetableDplaFields: Seq[String] =
    fields
      .filter(_.facetable)
      .filter(_.elasticSearchNotAnalyzed.nonEmpty)
      .map(_.name)

  def sortableDplaFields: Seq[String] =
    fields
      .filter(_.sortable)
      .filter(_.elasticSearchNotAnalyzed.nonEmpty)
      .map(_.name)

  def getElasticSearchField(name: String): Option[String] =
    fields.find(_.name == name).map(_.elasticSearchDefault)

  def getElasticSearchNotAnalyzed(name: String): Option[String] =
    fields.find(_.name == name).flatMap(_.elasticSearchNotAnalyzed)

  /**
   * Map DPLA MAP field to ElasticSearch non-analyzed field.
   * If a field is only indexed as analyzed (text), then return analyzed field.
   * Used for exact field matches and facets.
   */
  def getElasticSearchExactMatchField(name: String): Option[String] =
    fields.find(_.name == name)
      .map(field =>
        field.elasticSearchNotAnalyzed.getOrElse(field.elasticSearchDefault)
      )

  def getDplaFieldType(name: String): Option[DplaFieldType] =
    fields.find(_.name == name).map(_.fieldType)
}
