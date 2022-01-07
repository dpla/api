package dpla.publications

/**
 * Validates user-supplied parameters and provides default values.
 */
object ParamValidator {

  def getSearchParams(page: Option[String], pageSize: Option[String]): SearchParams =
    SearchParams(
      page = validPage(page),
      pageSize = validPageSize(pageSize)
    )

  private def toIntOpt(str: String): Option[Int] =
    try {
      Some(str.toInt)
    } catch {
      case _: Exception => None
    }

  // Must be an integer greater than 0, defaults to 1
  private def validPage(page: Option[String]): Int =
    page.flatMap(toIntOpt) match {
      case Some(int) =>
        if (int == 0) 1 else int
      case None => 1
    }

  // Must be an integer between 0 and 1000, defaults to 10
  private def validPageSize(pageSize: Option[String]): Int =
    pageSize.flatMap(toIntOpt) match {
      case Some(int) =>
        if (int > 1000) 1000 else int
      case None => 10
    }
}

case class SearchParams(
                         page: Int,
                         pageSize: Int
                       ) {
  // ElasticSearch param that defines the number of hits to skip
  def from: Int = (page-1)*pageSize

  // DPLA MAP field that gives the index of the first result on the page (starting at 1)
  def start: Int = from+1
}
