package dpla.ebookapi.v1.ebooks

import java.net.URL
import scala.util.{Failure, Success, Try}

/**
 * Validates user-supplied parameters and provides default values.
 */
object ParamValidator extends MappingHelper {

  private val defaultExactFieldMatch: Boolean = false
  private val defaultFacetSize: Int = 50
  private val minFacetSize: Int = 0
  private val maxFacetSize: Int = 2000
  private val defaultPage: Int = 1
  private val minPage: Int = 1
  private val maxPage: Int = 1000
  private val defaultPageSize: Int = 10
  private val minPageSize: Int = 1
  private val maxPageSize: Int = 1000

  // All valid user-submitted parameters for a search query
  private val acceptedSearchParams = Seq(
    "dataProvider",
    "exact_field_match",
    "facets",
    "facet_size",
    "isShownAt",
    "object",
    "page",
    "page_size",
    "q",
    "sourceResource.creator",
    "sourceResource.date.displayDate",
    "sourceResource.description",
    "sourceResource.format",
    "sourceResource.language.name",
    "sourceResource.publisher",
    "sourceResource.subject.name",
    "sourceResource.subtitle",
    "sourceResource.title"
  )

  // Method returns Failure if any parameters are invalid
  // Ebook ID must be a non-empty String comprised of letters, numbers, and hyphens
  def getValidId(id: String): Try[String] = Try {
    // TODO TEST PARAMS

    val rule = "ID must be a String comprised of letters, numbers, and hyphens between 1 and 32 characters long"

    if (id.length < 1 || id.length > 32) throw ValidationException(rule)
    else if (id.matches("[a-zA-Z0-9-]*")) id
    else throw ValidationException(rule)
  }

  /**
   * Method returns Failure if any parameters are invalid.
   */
  def getSearchParams(rawParams: Map[String, String]): Try[SearchParams] = Try {
    // TODO TEST ME
    // Check for unrecognized params
    val unrecognizedParams = rawParams.keys.toSeq diff acceptedSearchParams
    if (unrecognizedParams.nonEmpty)
      throw ValidationException("Unrecognized parameter: " + unrecognizedParams.mkString(", "))

    // Collect all the user-submitted field filters.
    val filters: Seq[FieldFilter] = Seq(
      getValidFieldFilter(rawParams, "dataProvider", validUrl),
      getValidFieldFilter(rawParams, "isShownAt", validUrl),
      getValidFieldFilter(rawParams, "object", validUrl),
      getValidFieldFilter(rawParams, "sourceResource.creator", validText),
      getValidFieldFilter(rawParams, "sourceResource.date.displayDate", validText),
      getValidFieldFilter(rawParams, "sourceResource.description", validText),
      getValidFieldFilter(rawParams, "sourceResource.format", validText),
      getValidFieldFilter(rawParams, "sourceResource.language.name", validText),
      getValidFieldFilter(rawParams, "sourceResource.publisher", validText),
      getValidFieldFilter(rawParams, "sourceResource.subject.name", validText),
      getValidFieldFilter(rawParams, "sourceResource.subtitle", validText),
      getValidFieldFilter(rawParams, "sourceResource.title", validText)
    ).flatten

    // Return valid field values. Provide defaults when appropriate.
    SearchParams(
      exactFieldMatch = getValid(rawParams, "exact_field_match", validBoolean)
        .getOrElse(defaultExactFieldMatch),
      facets = getValid(rawParams, "facets", validFields),
      facetSize = getValid(rawParams, "facet_size", validInt).getOrElse(defaultFacetSize),
      filters = filters,
      page = getValid(rawParams, "page", validInt).getOrElse(defaultPage),
      pageSize = getValid(rawParams, "page_size", validInt).getOrElse(defaultPageSize),
      q = getValid(rawParams, "q", validText)
    )
  }

  // Finds the raw parameter with the given name. Then validates with the given method.
  private def getValid[T](rawParams: Map[String, String],
                          paramName: String,
                          validationMethod: (String, String) => T): Option[T] =
    rawParams.find(_._1 == paramName).map{case (k,v) => validationMethod(v,k)}

  // Gets a valid value for a field filter.
  private def getValidFieldFilter(rawParams: Map[String, String],
                                  paramName: String,
                                  validationMethod: (String, String) => String): Option[FieldFilter] =
    getValid(rawParams, paramName, validationMethod).map(FieldFilter(paramName, _))

  // Must be a Boolean value.
  private def validBoolean(boolString: String, param: String): Boolean =
    boolString.toBooleanOption match {
      case Some(bool) => bool
      case None => throw ValidationException(s"$param must be a Boolean value")
    }

  // Must be in the list of accepted fields for the given param
  private def validFields(fieldString: String, param: String): Seq[String] = {
    val acceptedFields = param match {
      case "facets" => facetableDplaFields
      case _ => Seq[String]()
    }

    val filtered = fieldString.split(",").map(candidate => {
      if (acceptedFields.contains(candidate)) candidate
      else throw ValidationException(s"$candidate is not a recognized field for $param")
    })

    if (filtered.nonEmpty) filtered
    else throw ValidationException(s"$param must contain at least one valid field")
  }

  // Must be an integer between the min and the max for the given param.
  def validInt(intString: String, param: String): Int = {
    val (min: Int, max: Int) = param match {
      case "facet_size" => (minFacetSize, maxFacetSize)
      case "page" => (minPage, maxPage)
      case "page_size" => (minPageSize, maxPageSize)
      case _ => (0, 2147483647)
    }

    val rule = s"$param must be an integer between 0 and $max"

    Try(intString.toInt).toOption match {
      case Some(int) =>
        if (int < min || int > max) throw ValidationException(rule)
        else int
      case None =>
        // not an integer
        throw ValidationException(rule)
    }
  }

  // Must be a string between 2 and 200 characters
  private def validText(text: String, param: String): String =
    if (text.length < 2 || text.length > 200)
      // In the DPLA API (cultural heritage), an exception is thrown if q is too long, but not if q is too short.
      // For internal consistency, and exception is thrown here in both cases.
      throw ValidationException(s"$param must be between 2 and 200 characters")
    else text

  // Must be a valid URL
  private def validUrl(url: String, param: String): String = {
    val clean: String = {
      // Strip leading & trailing quotation marks for the purpose of checking for valid URL
      if (url.startsWith("\"") && url.endsWith("\""))
        url.stripSuffix("\"").stripPrefix("\"")
      else url
    }

    Try {
      new URL(clean)
    } match {
      case Success(_) => url // return value with leading & trailing quotation marks in tact
      case Failure(_) => throw ValidationException(s"$param must be a valid URL")
    }
  }
}
