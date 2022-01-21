package dpla.ebookapi.v1.ebooks

/** Case classes for search params */

case class RawParams(
                      creator: Option[String],
                      dataProvider: Option[String],
                      date: Option[String],
                      description: Option[String],
                      exactFieldMatch: Option[String],
                      facets: Option[String],
                      facetSize: Option[String],
                      isShownAt: Option[String],
                      format: Option[String],
                      language: Option[String],
                      `object`: Option[String],
                      page: Option[String],
                      pageSize: Option[String],
                      publisher: Option[String],
                      q: Option[String],
                      subject: Option[String],
                      subtitle: Option[String],
                      title: Option[String]
                    )

case class SearchParams(
                         exactFieldMatch: Boolean,
                         facets: Option[Seq[String]],
                         facetSize: Int,
                         filters: Seq[FieldFilter],
                         page: Int,
                         pageSize: Int,
                         q: Option[String]
                       ) {
  // ElasticSearch param that defines the number of hits to skip
  def from: Int = (page-1)*pageSize

  // DPLA MAP field that gives the index of the first result on the page (starting at 1)
  def start: Int = from+1
}

case class FieldFilter(
                        fieldName: String,
                        value: String
                      )

final case class ValidationException(private val message: String = "") extends Exception(message)

/** Case classes for reading ElasticSearch responses **/

case class SingleEbook(
                        docs: Seq[Ebook]
                      )

case class EbookList(
                      count: Option[Int],
                      limit: Option[Int],
                      start: Option[Int],
                      docs: Seq[Ebook],
                      facets: Option[FacetList]
                    )

case class Ebook(
                  author: Seq[String],
                  genre: Seq[String],
                  id: Option[String],
                  itemUri: Option[String],
                  language: Seq[String],
                  medium: Seq[String],
                  payloadUri: Seq[String],
                  publisher: Seq[String],
                  publicationDate: Seq[String],
                  sourceUri: Option[String],
                  subtitle: Seq[String],
                  summary: Seq[String],
                  title: Seq[String]
                )

case class FacetList(
                      facets: Seq[Facet]
                    )

case class Facet(
                  field: String,
                  buckets: Seq[Bucket]
                )

case class Bucket(
                   key: Option[String],
                   docCount: Option[Int]
                 )
