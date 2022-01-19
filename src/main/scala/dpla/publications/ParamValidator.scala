package dpla.publications

import scala.util.{Failure, Success, Try}
import java.net.URL

/**
 * Validates user-supplied parameters and provides default values.
 */
object ParamValidator extends MappingHelper {

  // Method returns Failure if any parameters are invalid
  def getSearchParams(raw: RawParams): Try[SearchParams] = Try{

    // Get valid field values
    // These methods will throw InvalidParameterException if the field is not valid
    val creator: Option[String] = validText(raw.creator, rawParamToDpla("creator"))
    val dataProvider: Option[String] = validUrl(raw.dataProvider, rawParamToDpla("dataProvider"))
    val date: Option[String] = validText(raw.date, rawParamToDpla("date"))
    val description: Option[String] = validText(raw.description, rawParamToDpla("description"))
    val exactFieldMatch: Boolean = validExactFieldMatch(raw.exactFieldMatch)
    val facets: Option[Seq[String]] = validFacets(raw.facets)
    val facetSize: Int = validFacetSize(raw.facetSize)
    val format: Option[String] = validText(raw.format, rawParamToDpla("format"))
    val isShownAt: Option[String] = validUrl(raw.isShownAt, rawParamToDpla("isShownAt"))
    val language: Option[String] = validText(raw.language, rawParamToDpla("language"))
    val `object`: Option[String] = validUrl(raw.`object`, rawParamToDpla("object"))
    val page: Int = validPage(raw.page)
    val pageSize: Int = validPageSize(raw.pageSize)
    val publisher: Option[String] = validText(raw.publisher, rawParamToDpla("publisher"))
    val q: Option[String] = validText(raw.q, "q")
    val subject: Option[String] = validText(raw.subject, rawParamToDpla("subject"))
    val subtitle: Option[String] = validText(raw.subtitle, rawParamToDpla("subtitle"))
    val title: Option[String] = validText(raw.title, rawParamToDpla("title"))

    // Collect all of the user-submitted field filters
    val filters = Seq(
      FieldFilter(rawParamToDpla("creator"), creator.getOrElse("")),
      FieldFilter(rawParamToDpla("dataProvider"), dataProvider.getOrElse("")),
      FieldFilter(rawParamToDpla("date"), date.getOrElse("")),
      FieldFilter(rawParamToDpla("description"), description.getOrElse("")),
      FieldFilter(rawParamToDpla("format"), format.getOrElse("")),
      FieldFilter(rawParamToDpla("isShownAt"), isShownAt.getOrElse("")),
      FieldFilter(rawParamToDpla("language"), language.getOrElse("")),
      FieldFilter(rawParamToDpla("object"), `object`.getOrElse("")),
      FieldFilter(rawParamToDpla("publisher"), publisher.getOrElse("")),
      FieldFilter(rawParamToDpla("subject"), subject.getOrElse("")),
      FieldFilter(rawParamToDpla("subtitle"), subtitle.getOrElse("")),
      FieldFilter(rawParamToDpla("title"), title.getOrElse("")),
    ).filter(_.value.nonEmpty)

    SearchParams(
      exactFieldMatch = exactFieldMatch,
      facets = facets,
      facetSize = facetSize,
      filters = filters,
      page = page,
      pageSize = pageSize,
      q = q
    )
  }

  private def toIntOpt(str: String): Option[Int] =
    Try(str.toInt).toOption

  // Must be a Boolean value. Default is false.
  private def validExactFieldMatch(exactFieldMatch: Option[String]): Boolean =
    exactFieldMatch match {
      case Some(efm) =>
        efm.toBooleanOption match {
          case Some(bool) => bool
          case None => throw ValidationException("exact_field_match must be a Boolean value")
        }
      case None =>
        false
    }

  // Must be a facetable field
  private def validFacets(facets: Option[String]): Option[Seq[String]] =
    facets match {
      case Some(f) =>
        val filtered = f.split(",").map(candidate => {
          if (facetableDplaFields.contains(candidate)) candidate
          else throw ValidationException(s"$candidate is not a facetable field")
        })
        if (filtered.nonEmpty) Some(filtered) else None
      case None => None
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
  private def validUrl(url: Option[String], label: String): Option[String] =
    url match {
      case Some(maybeUrl) =>
        val clean: String = {
          // Strip leading & trailing quotation marks for the purpose of checking for valid URL
          if (maybeUrl.startsWith("\"") && maybeUrl.endsWith("\""))
            maybeUrl.stripSuffix("\"").stripPrefix("\"")
          else maybeUrl
        }

        Try {
          new URL(clean)
        } match {
          case Success(_) => url // return value with leading & trailing quotation marks in tact
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
                    ) {
}

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
