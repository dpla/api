package dpla.publications

import scala.util.{Failure, Success, Try}
import java.net.URL

/**
 * Validates user-supplied parameters and provides default values.
 */
object ParamValidator {

  // Method returns Failure if any parameters are invalid
  def getSearchParams(raw: RawParams): Try[SearchParams] = Try(
    SearchParams(
      creator = validText(raw.creator, "sourceResource.creator"),
      dataProvider = validUrl(raw.dataProvider, "dataProvider"),
      date = validText(raw.date, "sourceResource.date.displayDate"),
      description = validText(raw.description, "sourceResource.description"),
      facets = validFacets(raw.facets),
      facetSize = validFacetSize(raw.facetSize),
      format = validText(raw.format, "sourceResource.format"),
      isShownAt = validUrl(raw.isShownAt, "isShownAt"),
      language = validText(raw.language, "sourceResource.language"),
      `object` = validUrl(raw.`object`, "object"),
      page = validPage(raw.page),
      pageSize = validPageSize(raw.pageSize),
      publisher = validText(raw.publisher, "sourceResource.publisher"),
      q = validText(raw.q, "q"),
      subject = validText(raw.subject, "sourceResource.subject.name"),
      subtitle = validText(raw.subtitle, "sourceResource.subtitle"),
      title = validText(raw.title, "sourceResource.title")
    ))

  private def toIntOpt(str: String): Option[Int] =
    Try(str.toInt).toOption

  // Must be a facetable field
  // Facetable fields must be indexed as type "keyword" in ElasticSearch
  private def validFacets(facets: Option[String]): Option[Seq[String]] = {
    val facetableFields: Seq[String] = Seq(
      "dataProvider"
    )

    facets match {
      case Some(f) =>
        val filtered = f.split(",").map(candidate => {
          if (facetableFields.contains(candidate)) candidate
          else throw ValidationException(s"$candidate is not a facetable field")
        })
        if (filtered.nonEmpty) Some(filtered) else None
      case None => None
    }
  }

  // Must be an integer between 0 and 2000, defaults to 50
  def validFacetSize(facetSize: Option[String]): Int = {
    val facetSizeRule = "facet_size must be an integer between 0 and 2000"

    facetSize match {
      case Some(f) =>
        toIntOpt(f) match {
          case Some(int) =>
            if (int > 2000) throw ValidationException(facetSizeRule)
            else int
          case None =>
            // not an integer
            throw ValidationException(facetSizeRule)
        }
      case None => 50
    }
  }

  // Must be an integer greater than 0, defaults to 1
  private def validPage(page: Option[String]): Int = {
    val pageRule = "page must be an integer greater than 0"

    page match {
      case Some(p) =>
        toIntOpt(p) match {
          case Some(int) =>
            if (int == 0) throw ValidationException(pageRule)
            else int
          case None =>
            // not an integer
            throw new ValidationException(pageRule)
        }
      case None => 1
    }
  }

  // Must be an integer between 0 and 1000, defaults to 10
  private def validPageSize(pageSize: Option[String]): Int = {
    val pageSizeRule = "page_size must be an integer between 0 an 1000"

    pageSize match {
      case Some(p) =>
        toIntOpt(p) match {
          case Some(int) =>
            if (int > 1000) throw new ValidationException(pageSizeRule)
            else int
          case None =>
            // not an integer
            throw new ValidationException(pageSizeRule)
        }
      case None => 10
    }
  }

  // Must be a string between 2 and 200 characters
  private def validText(text: Option[String], label: String): Option[String] =
    text match {
      case Some(keyword) =>
        if (keyword.length < 2 || keyword.length > 200) {
          // In the DPLA API (cultural heritage), an exception is thrown if q is too long, but not if q is too short.
          // For internal consistency, and exception is thrown here in both cases.
          throw ValidationException(s"$label must be between 2 and 200 characters")
        } else Some(keyword)
      case None => None
    }

  // Must be a valid URL
  // TODO: Handle leading/trailing quotation marks?
  private def validUrl(url: Option[String], label: String): Option[String] =
    url match {
      case Some(maybeUrl) =>
        Try{new URL(maybeUrl)} match {
          case Success(_) => url
          case Failure(_) => throw ValidationException(s"$label must be a valid URL")
        }
      case None => None
    }
}

/** Case classes for search params */

case class RawParams(
                      creator: Option[String],
                      dataProvider: Option[String],
                      date: Option[String],
                      description: Option[String],
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
                    ) {
}

case class SearchParams(
                         creator: Option[String],
                         dataProvider: Option[String],
                         date: Option[String],
                         description: Option[String],
                         facets: Option[Seq[String]],
                         facetSize: Int,
                         format: Option[String],
                         isShownAt: Option[String],
                         language: Option[String],
                         `object`: Option[String],
                         page: Int,
                         pageSize: Int,
                         publisher: Option[String],
                         q: Option[String],
                         subject: Option[String],
                         subtitle: Option[String],
                         title: Option[String]
                       ) {
  // ElasticSearch param that defines the number of hits to skip
  def from: Int = (page-1)*pageSize

  // DPLA MAP field that gives the index of the first result on the page (starting at 1)
  def start: Int = from+1
}

final case class ValidationException(private val message: String = "") extends Exception(message)
