package dpla.api.v2.search.paramValidators

import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import akka.actor.typed.{ActorRef, Behavior}
import dpla.api.v2.search.SearchProtocol._
import dpla.api.v2.search.models.FieldDefinitions

import java.net.URL
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

/**
 * Validates user-submitted search and fetch parameters.
 * Provides default values when appropriate.
 * Bad actors may use invalid search params to try and hack the system, so they
 * are logged as warnings.
 */

/** Case classes for representing valid search parameters */

private[search] case class SearchParams(
                                         exactFieldMatch: Boolean,
                                         facets: Option[Seq[String]],
                                         facetSize: Int,
                                         fields: Option[Seq[String]],
                                         fieldQueries: Seq[FieldQuery],
                                         filter: Option[Seq[Filter]],
                                         op: String,
                                         page: Int,
                                         pageSize: Int,
                                         q: Option[String],
                                         sortBy: Option[String],
                                         sortByPin: Option[String],
                                         sortOrder: String
                                       )

private[search] case class FetchParams(
                                        fields: Option[Seq[String]] = None
                                      )

private[search] case class RandomParams(
                                        filter: Option[Seq[Filter]] = None
                                       )

private[search] case class FieldQuery(
                                       fieldName: String,
                                       value: String
                                      )

private[search] case class Filter(
                                   fieldName: String,
                                   value: String
                                 )

trait ParamValidator extends FieldDefinitions {

  def apply(
             nextPhase: ActorRef[IntermediateSearchResult]
           ): Behavior[IntermediateSearchResult] = {

    Behaviors.setup { context =>

      Behaviors.receiveMessage[IntermediateSearchResult] {

        case RawSearchParams(rawParams, replyTo) =>
          val preProcessed = preProcess(rawParams)
          getSearchParams(preProcessed) match {
            case Success(searchParams) =>
              nextPhase ! ValidSearchParams(searchParams, replyTo)
            case Failure(e) =>
              context.log.warn2(
                "Invalid search params: '{}' for params '{}'",
                e.getMessage,
                rawParams.map { case(key, value) => s"$key: $value"}.mkString(", ")
              )
              replyTo ! InvalidSearchParams(e.getMessage)
          }
          Behaviors.same

        case RawFetchParams(id, rawParams, replyTo) =>
          getFetchIds(id, rawParams) match {
            case Success(validIds) =>
              nextPhase ! ValidFetchParams(validIds, None, replyTo)
            case Failure(e) =>
              context.log.warn2(
                "Invalid fetch params: '{}' for params '{}'",
                e.getMessage,
                rawParams
                  .map { case(key, value) => s"$key: $value"}
                  .++(Map("id" -> id))
                  .mkString(", ")
              )
              replyTo ! InvalidSearchParams(e.getMessage)
          }
          Behaviors.same

        case RawRandomParams(rawParams, replyTo) =>
          getRandomParams(rawParams) match {
            case Success(validParams) =>
              nextPhase ! ValidRandomParams(validParams, replyTo)
            case Failure(e) =>
              context.log.warn2(
                "Invalid random params: '{}' for params '{}'",
                e.getMessage,
                rawParams.map { case(key, value) => s"$key: $value"}.mkString(", ")
              )
              replyTo ! InvalidSearchParams(e.getMessage)
          }
          Behaviors.same

        case _ =>
          Behaviors.unhandled
      }
    }
  }

  /** Abstract Methods */

  protected def preProcess(value: Map[String, String]): Map[String, String]

  protected val defaultExactFieldMatch: Boolean = false
  protected val defaultFacetSize: Int = 50
  protected val minFacetSize: Int = 0
  protected val maxFacetSize: Int = 2000
  protected val defaultOp: String = "AND"
  protected val defaultPage: Int = 1
  protected val minPage: Int = 1
  protected val maxPage: Int = 100
  protected val defaultPageSize: Int = 10
  protected val minPageSize: Int = 0
  protected val maxPageSize: Int = 500
  protected val defaultSortOrder: String = "asc"

  // Abstract.
  // These parameters are valid for a search request.
  protected val acceptedSearchParams: Seq[String]

  // Abstract.
  // These parameters are valid for a fetch request.
  protected val acceptedFetchParams: Seq[String]

  // Abstract.
  // These fields are not valid for search, sort, filter, & facets.
  // Rather than returning an error, they should be ignored.
  protected val ignoredFields: Seq[String]

  private case class ValidationException(
                                          private val message: String = ""
                                        ) extends Exception(message)

  /**
   * Get valid fetch params.
   * Fails with ValidationException if id or any raw params are invalid.
   */
  private def getFetchIds(
                           id: String,
                           rawParams: Map[String, String]
                         ): Try[Seq[String]] =
    Try {
      // There are no recognized params for a fetch request
      if (rawParams.nonEmpty)
        throw ValidationException(
          "Unrecognized parameter: " + rawParams.keys.mkString(", ")
        )
      else {
        val ids = id.split(",")
        if (ids.size > maxPageSize) throw ValidationException(
          s"The number of ids cannot exceed $maxPageSize"
        )
        ids.map(getValidId)
      }
    }

  /**
   * Get valid search params.
   * Fails with ValidationException if any raw params are invalid.
   */
  private def getSearchParams(rawParams: Map[String, String]): Try[SearchParams] =
    Try {
      // Check for unrecognized params
      val unrecognized = rawParams.keys.toSeq diff acceptedSearchParams

      if (unrecognized.nonEmpty)
        throw ValidationException(
          "Unrecognized parameter: " + unrecognized.mkString(", ")
        )
      else {
        // Check for valid search params

        // Collect all the user-submitted field queries.
        val fieldQueries: Seq[FieldQuery] =
          searchableDataFields.flatMap(getValidFieldQuery(rawParams, _))

        // Return valid search params. Provide defaults when appropriate.
        SearchParams(
          exactFieldMatch =
            getValid(rawParams, "exact_field_match", validBoolean)
              .getOrElse(defaultExactFieldMatch),
          facets =
            getValid(rawParams, "facets", validFields),
          facetSize =
            getValid(rawParams, "facet_size", validIntWithRange)
              .getOrElse(defaultFacetSize),
          fields =
            getValid(rawParams, "fields", validFields),
          fieldQueries =
            fieldQueries,
          filter =
            getValidFilter(rawParams),
          op =
            getValid(rawParams, "op", validAndOr)
              .getOrElse(defaultOp),
          page =
            getValid(rawParams, "page", validIntWithRange)
              .getOrElse(defaultPage),
          pageSize =
            getValid(rawParams, "page_size", validIntWithRange)
              .getOrElse(defaultPageSize),
          q =
            getValid(rawParams, "q", validText),
          sortBy =
            getValidSortField(rawParams),
          sortByPin =
            getValidSortByPin(rawParams),
          sortOrder =
            getValid(rawParams, "sort_order", validSortOrder)
              .getOrElse(defaultSortOrder)
        )
      }
    }

  private def getRandomParams(rawParams: Map[String, String]): Try[RandomParams] =
    Try {
      // Check for unrecognized params
      val unrecognized = rawParams.keys.toSeq diff Seq("filter")

      if (unrecognized.nonEmpty)
        throw ValidationException(
          "Unrecognized parameter: " + unrecognized.mkString(", ")
        )
      else {
        // Check for valid filter
        val filter = getValidFilter(rawParams)

        RandomParams(filter)
      }
    }

  // Look up the parameter's field type.
  // Use this to determine the appropriate validation method.
  def getValidationMethod(paramName: String): (String, String) => String =
    getDataFieldType(paramName) match {
      case Some(fieldType) =>
        fieldType match {
          case TextField => validText
          case IntField => validInt
          case URLField => validUrl
          case DateField => validDate
          case WildcardField => validText
          case _ => validText
        }
      case None =>
        throw ValidationException(s"Unrecognized parameter: $paramName")
    }

  /**
   * Method returns Failure if ID is invalid.
   * Ebook ID must be a non-empty String comprised of letters, numbers, and
   * hyphens.
   */
  private def getValidId(id: String): String = {
    val rule = "ID must be a String comprised of letters, numbers, and " +
      "hyphens between 1 and 32 characters long"

    if (id.length < 1 || id.length > 32) throw ValidationException(rule)
    else if (id.matches("[a-zA-Z0-9-]*")) id
    else throw ValidationException(rule)
  }

  /**
   * Get a valid value for a field query.
   */
  private def getValidFieldQuery(rawParams: Map[String, String],
                                 paramName: String): Option[FieldQuery] = {

    val validationMethod = getValidationMethod(paramName)

    getValid(rawParams, paramName, validationMethod)
      .map(FieldQuery(paramName, _))
  }

  /**
   * Get a valid field name and value for a filter query.
   */
  private def getValidFilter(rawParams: Map[String, String]): Option[Seq[Filter]] =
    rawParams.get("filter").map{ filter =>

      val fieldName = filter.split(":", 2).headOption
        .getOrElse(throw ValidationException(s"$filter is not a valid filter"))

      val values = filter.split(":", 2).lastOption
        .getOrElse(throw ValidationException(s"$filter is not a valid filter"))
        .split("AND").map(_.trim)

      if (searchableDataFields.contains(fieldName)) {
        val validationMethod = getValidationMethod(fieldName)

        values.flatMap(value => {
          val params = Map(fieldName -> value)
          getValid(params, fieldName, validationMethod)
            .map(Filter(fieldName, _))
        })
      } else {
        throw ValidationException(s"$fieldName is not a valid filter field")
      }
    }

  /**
   * Get a valid value for sort_by parameter.
   * Must be in the list of sortable fields.
   * If coordinates, query must also contain the "sort_by_pin" parameter.
   */
  private def getValidSortField(rawParams: Map[String, String]): Option[String] =
    rawParams.get("sort_by").map{ sortField =>
      // Check if field is sortable according to the field definition
      if (sortableDataFields.contains(sortField)) {
        // Check if field represents coordinates
        if (coordinatesField.map(_.name).contains(sortField))
          // Check if sort_by_pin is an accepted search param for this validator
          if (acceptedSearchParams.contains("sort_by_pin"))
            // Check if raw params also contains sort_by_pin
            rawParams.get("sort_by_pin") match {
              case Some(_) => sortField
              case None =>
                throw ValidationException(
                  "The sort_by_pin parameter is required."
                )
            }
          else
            throw ValidationException(
              s"'$sortField' is not an allowable value for sort_by"
            )
        else sortField
      } else
        throw ValidationException(
          s"'$sortField' is not an allowable value for sort_by"
        )
    }

  /**
   * Get valid value for sort_by_pin.
   * Query must also contain the "sort_by" parameter with the coordinates field.
   */
  private def getValidSortByPin(rawParams: Map[String, String]): Option[String] = {
    rawParams.get("sort_by_pin").map { coordinates =>
      // Check if field is valid text (will throw exception if not)
      val validCoordinates: String = validText(coordinates, "sort_by_pin")
      // Check if raw params also contains "sort_by" with coordinates field
      rawParams.get("sort_by") match {
        case Some(sortBy) =>
          if (coordinatesField.map(_.name).contains(sortBy))
            validCoordinates
          else
            throw ValidationException(
              s"The sort_by parameter is required."
            )
        case None =>
          throw ValidationException(
              s"The sort_by parameter is required."
          )
      }
    }
  }

  /**
   * Find the raw parameter with the given name.
   * Then validate with the given method.
   */
  private def getValid[T](rawParams: Map[String, String],
                          paramName: String,
                          validationMethod: (String, String) => T): Option[T] =
    rawParams.find(_._1 == paramName).map{case (k,v) => validationMethod(v,k)}

  // Must be a Boolean value.
  private def validBoolean(boolString: String, param: String): Boolean =
    boolString.toBooleanOption match {
      case Some(bool) => bool
      case None => throw ValidationException(s"$param must be a Boolean value")
    }

  // One or more fields.
  // Must be in the list of accepted fields for the given param.
  protected def validFields(fieldString: String, param: String): Seq[String] = {
    val acceptedFields = param match {
      case "facets" => facetableDataFields
      case "fields" => allDataFields
      case _ => Seq[String]()
    }

    fieldString.split(",").flatMap(candidate => {
      // Need to check ignoredFields first b/c acceptedFields may contain
      // fields that are also in ignoredFields
      if (ignoredFields.contains(candidate))
        None
      else if (acceptedFields.contains(candidate))
        Some(candidate)
      else if (param == "facets" && coordinatesField.map(_.name)
        .contains(candidate.split(":").head))

        Some(candidate)
      else
        throw ValidationException(
          s"'$candidate' is not an allowable value for '$param'"
        )
    })
  }

  // Must be an integer
  private def validInt(int: String, param: String): String =
    Try { int.toInt } match {
      case Success(_) => int
      case Failure(_) =>
        throw ValidationException(s"$param must be an integer")
    }

  // Must be an integer between the min and the max for the given param.
  def validIntWithRange(intString: String, param: String): Int = {
    val (min: Int, max: Int) = param match {
      case "facet_size" => (minFacetSize, maxFacetSize)
      case "page" => (minPage, maxPage)
      case "page_size" => (minPageSize, maxPageSize)
      case _ => (0, 2147483647)
    }

    val rule = s"$param must be an integer between 0 and $max"

    Try(intString.toInt).toOption match {
      case Some(int) =>
        if (int < min) throw ValidationException(rule)
        else if (int > max) max
        else int
      case None =>
        // not an integer
        throw ValidationException(rule)
    }
  }

  // Must be a string between 2 and 200 characters.
  private def validText(text: String, param: String): String =
    if (text.length < 2 || text.length > 200)
    // In the DPLA API (cultural heritage), an exception is thrown if q is too
    // long, but not if q is too short.
    // For internal consistency, and exception is thrown here in both cases.
      throw ValidationException(s"$param must be between 2 and 200 characters")
    else text

  // Must be in the format YYYY, YYYY-MM, or YYYY-MM-DD
  private def validDate(text: String, param: String): String = {
    val rule = s"$param must be in the form YYYY or YYYY-MM or YYYY-MM-DD"

    val year: Regex = """\d{4}""".r
    val yearMonth: Regex = raw"""\d{4}-\d{2}""".r
    val yearMonthDay: Regex = raw"""\d{4}-\d{2}-\d{2}""".r

    if (year.matches(text) || yearMonth.matches(text) || yearMonthDay.matches(text))
      text
    else
      throw ValidationException(rule)
  }

  // Must be a valid URL.
  private def validUrl(url: String, param: String): String = {
    val clean: String = {
      // Strip leading & trailing quotation marks for the purpose of checking
      // for valid URL
      if (url.startsWith("\"") && url.endsWith("\""))
        url.stripSuffix("\"").stripPrefix("\"")
      else url
    }

    Try {
      new URL(clean)
    } match {
      case Success(_) =>
        // return value with leading & trailing quotation marks in tact
        url
      case Failure(_) =>
        throw ValidationException(s"$param must be a valid URL")
    }
  }

  // Must be 'AND' or 'OR'
  private def validAndOr(andor: String, param: String): String =
    if (andor == "AND" || andor == "OR") andor
    else throw ValidationException(s"$param must be 'AND' or 'OR'")

  // Must be 'asc' or 'desc'
  private def validSortOrder(order: String, param: String): String =
    if (order == "asc" || order == "desc") order
    else throw ValidationException(s"$param must be 'asc' or 'desc'")
}
