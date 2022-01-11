package dpla.publications

import java.security.InvalidParameterException
import scala.util.Try

/**
 * Validates user-supplied parameters and provides default values.
 */
object ParamValidator {

  // Method returns Failure if any parameters are invalid
  def getSearchParams(raw: RawParams): Try[SearchParams] = Try(
    SearchParams(
      facets = validFacets(raw.facets),
      facetSize = validFacetSize(raw.facetSize),
      page = validPage(raw.page),
      pageSize = validPageSize(raw.pageSize),
      q = validQ(raw.q)
    ))

  private def toIntOpt(str: String): Option[Int] =
    Try(str.toInt).toOption

  // Must be a facetable field
  private def validFacets(facets: Option[String]): Option[Seq[String]] = {
    val facetableFields: Seq[String] = Seq(
      "dataProvider",
      "sourceResource.creator",
      "sourceResource.format",
      "sourceResource.language.name",
      "sourceResource.publisher",
      "sourceResource.subject.name"
    )

    facets match {
      case Some(f) =>
        val filtered = f.split(",").map(candidate => {
          if (facetableFields.contains(candidate)) candidate
          else throw new InvalidParameterException(s"$candidate is not a facetable field")
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
            if (int > 2000) throw new InvalidParameterException(facetSizeRule)
            else int
          case None =>
            // not an integer
            throw new InvalidParameterException(facetSizeRule)
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
            if (int == 0) throw new InvalidParameterException(pageRule)
            else int
          case None =>
            // not an integer
            throw new InvalidParameterException(pageRule)
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
            if (int > 1000) throw new InvalidParameterException(pageSizeRule)
            else int
          case None =>
            // not an integer
            throw new InvalidParameterException(pageSizeRule)
        }
      case None => 10
    }
  }

  // Must be a string between 2 and 200 characters
  private def validQ(q: Option[String]): Option[String] =
    q  match {
      case Some(keyword) =>
        if (keyword.length < 2 || keyword.length > 200) {
          // In the DPLA API (cultural heritage), an exception is thrown if q is too long, but not if q is too short.
          // For internal consistency, and exception is thrown here in both cases.
          throw new InvalidParameterException("q must be between 2 and 200 characters")
        } else Some(keyword)
      case None => None
    }
}

/** Case classes for search params */

case class RawParams(
                      facets: Option[String],
                      facetSize: Option[String],
                      page: Option[String],
                      pageSize: Option[String],
                      q: Option[String]
                    )

case class SearchParams(
                         facets: Option[Seq[String]],
                         facetSize: Int,
                         page: Int,
                         pageSize: Int,
                         q: Option[String]
                       ) {
  // ElasticSearch param that defines the number of hits to skip
  def from: Int = (page-1)*pageSize

  // DPLA MAP field that gives the index of the first result on the page (starting at 1)
  def start: Int = from+1

  // ElasticSearch param that defines aggregations (i.e. facets)
  def aggFields: Option[Seq[String]] = facets match {
    case Some(f) =>
      val mapped = f.flatMap(_ match {
        case "dataProvider" => Some("sourceUri")
        case "sourceResource.creator" => Some("author")
        case "sourceResource.format" => Some("medium")
        case "sourceResource.language.name" => Some("language")
        case "sourceResource.publisher" => Some("publisher")
        case "sourceResource.subject.name" => Some("genre")
        case _ => None
      })
      if (mapped.nonEmpty) Some(mapped) else None
    case None => None
  }
}
