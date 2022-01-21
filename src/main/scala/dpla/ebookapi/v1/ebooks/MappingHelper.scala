package dpla.ebookapi.v1.ebooks

/**
 * Maps between DPLA MAP field names and ElasticSearch field names.
 */
trait MappingHelper {

  /**
   * Map DPLA MAP fields to ElasticSearch fields
   * Fields are either analyzed (text) or not (keyword) as is their default in the index
   */
//  def dplaToElasticSearch(dplaField: String): String =
//    mapDplaToEs(dplaField)
//
//  /**
//   * Map DPLA MAP fields to ElasticSearch non-analyzed fields.
//   * If a field is only indexed as analyzed (text), then return the analyzed field.
//   * Used for exact field matches and facets.
//   */
//  def dplaToElasticSearchExactMatch(dplaField: String): String =
//    mapDplaToEsExactMatch(dplaField)

  private val mapDplaToEs = Map(
    "dataProvider" -> "sourceUri",
    "isShownAt" -> "itemUri",
    "object" -> "payloadUri",
    "sourceResource.creator" -> "author",
    "sourceResource.date.displayDate" -> "publicationDate",
    "sourceResource.description" -> "summary",
    "sourceResource.format" -> "medium",
    "sourceResource.language.name" -> "language",
    "sourceResource.publisher" -> "publisher",
    "sourceResource.subject.name" -> "genre",
    "sourceResource.subtitle" -> "subtitle",
    "sourceResource.title" -> "title"
  )

  private val mapDplaToEsExactMatch = Map(
    "dataProvider" -> "sourceUri",
    "isShownAt" -> "itemUri",
    "object" -> "payloadUri",
    "sourceResource.creator" -> "author.not_analyzed",
    "sourceResource.date.displayDate" -> "publicationDate.not_analyzed",
    "sourceResource.description" -> "summary",
    "sourceResource.format" -> "medium.not_analyzed",
    "sourceResource.language.name" -> "language.not_analyzed",
    "sourceResource.publisher" -> "publisher.not_analyzed",
    "sourceResource.subject.name" -> "genre.not_analyzed",
    "sourceResource.subtitle" -> "subtitle.not_analyzed",
    "sourceResource.title" -> "title.not_analyzed"
  )
}
