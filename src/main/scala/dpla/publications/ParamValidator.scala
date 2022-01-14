package dpla.publications

import scala.util.{Failure, Success, Try}
import java.net.URL

/**
 * Validates user-supplied parameters and provides default values.
 */
object ParamValidator {

  // Method returns Failure if any parameters are invalid
  def getSearchParams(raw: RawParams): Try[SearchParams] = Try{

    // Get valid field values
    // These methods will throw InvalidParameterException if the field is not valid
    val creator: Option[String] = validText(raw.creator, getDplaField("creator"))
    val dataProvider: Option[String] = validUrl(raw.dataProvider, getDplaField("dataProvider"))
    val date: Option[String] = validText(raw.date, getDplaField("date"))
    val description: Option[String] = validText(raw.description, getDplaField("description"))
    val exactFieldMatch: Boolean = validExactFieldMatch(raw.exactFieldMatch)
    val facets: Option[Seq[String]] = validFacets(raw.facets)
    val facetSize: Int = validFacetSize(raw.facetSize)
    val format: Option[String] = validText(raw.format, getDplaField("format"))
    val isShownAt: Option[String] = validUrl(raw.isShownAt, getDplaField("isShownAt"))
    val language: Option[String] = validText(raw.language, getDplaField("language"))
    val `object`: Option[String] = validUrl(raw.`object`, getDplaField("object"))
    val page: Int = validPage(raw.page)
    val pageSize: Int = validPageSize(raw.pageSize)
    val publisher: Option[String] = validText(raw.publisher, getDplaField("publisher"))
    val q: Option[String] = validText(raw.q, "q")
    val subject: Option[String] = validText(raw.subject, getDplaField("subject"))
    val subtitle: Option[String] = validText(raw.subtitle, getDplaField("subtitle"))
    val title: Option[String] = validText(raw.title, getDplaField("title"))

    // Get all of the user-submitted field filters
    val filters = Seq(
      FieldFilter(getDplaField("creator"), creator.getOrElse("")),
      FieldFilter(getDplaField("dataProvider"), dataProvider.getOrElse("")),
      FieldFilter(getDplaField("date"), date.getOrElse("")),
      FieldFilter(getDplaField("description"), description.getOrElse("")),
      FieldFilter(getDplaField("format"), format.getOrElse("")),
      FieldFilter(getDplaField("isShownAt"), isShownAt.getOrElse("")),
      FieldFilter(getDplaField("language"), language.getOrElse("")),
      FieldFilter(getDplaField("object"), `object`.getOrElse("")),
      FieldFilter(getDplaField("publisher"), publisher.getOrElse("")),
      FieldFilter(getDplaField("subject"), subject.getOrElse("")),
      FieldFilter(getDplaField("subtitle"), subtitle.getOrElse("")),
      FieldFilter(getDplaField("title"), title.getOrElse("")),
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

  private def getDplaField(label: String): String = label match {
    case "creator" => "sourceResource.creator"
    case "dataProvider" => "dataProvider"
    case "date" => "sourceResource.date.displayDate"
    case "description" => "sourceResource.description"
    case "format" => "sourceResource.format"
    case "isShownAt" => "isShownAt"
    case "language" => "sourceResource.language.name"
    case "object" => "object"
    case "publisher" => "sourceResource.publisher"
    case "subject" => "sourceResource.subject.name"
    case "subtitle" => "sourceResource.subtitle"
    case "title" => "sourceResource.title"
    case _ => throw new RuntimeException("Unknown DPLA field")
  }

  // Facetable fields must be indexed as type "keyword" in ElasticSearch
  val facetableFields: Seq[String] = Seq(
    "dataProvider",
    "sourceResource.creator",
    "sourceResource.date",
    "sourceResource.format",
    "sourceResource.language.name",
    "sourceResource.publisher",
    "sourceResource.subject.name",
    "sourceResource.subtitle",
    "sourceResource.title"
  )

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
          if (facetableFields.contains(candidate)) candidate
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
  //
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
