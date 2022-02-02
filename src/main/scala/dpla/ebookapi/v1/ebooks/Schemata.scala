package dpla.ebookapi.v1.ebooks

/** Case classes for search params */

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




