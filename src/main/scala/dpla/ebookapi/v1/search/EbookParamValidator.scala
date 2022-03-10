package dpla.ebookapi.v1.search

import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import akka.actor.typed.{ActorRef, Behavior}
import dpla.ebookapi.v1.search.SearchProtocol.{ValidFetchId, ValidSearchParams, IntermediateSearchResult, InvalidSearchParams, RawFetchParams, RawSearchParams}

import java.net.URL
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
                                         filters: Seq[FieldFilter],
                                         op: String,
                                         page: Int,
                                         pageSize: Int,
                                         q: Option[String],
                                         sortBy: Option[String],
                                         sortOrder: String
                                       )

private[search] case class FieldFilter(
                                        fieldName: String,
                                        value: String
                                      )

object EbookParamValidator extends EbookFields {

  def apply(
             nextSearchPhase: ActorRef[IntermediateSearchResult],
             nextFetchPhase: ActorRef[IntermediateSearchResult]
           ): Behavior[IntermediateSearchResult] = {

    Behaviors.setup { context =>

      Behaviors.receiveMessage {

        case RawSearchParams(rawParams, replyTo) =>
          getSearchParams(rawParams) match {
            case Success(searchParams) =>
              nextSearchPhase ! ValidSearchParams(searchParams, replyTo)
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
          getFetchId(id, rawParams) match {
            case Success(validId) =>
              nextFetchPhase ! ValidFetchId(validId, replyTo)
            case Failure(e) =>
              context.log.warn2(
                "Invalid fetch params: '{}' params '{}'",
                e.getMessage,
                rawParams
                  .map { case(key, value) => s"$key: $value"}
                  .++(Map("id" -> id))
                  .mkString(", ")
              )
              replyTo ! InvalidSearchParams(e.getMessage)
          }
          Behaviors.same

        case _ =>
          Behaviors.unhandled
      }
    }
  }

  private val defaultExactFieldMatch: Boolean = false
  private val defaultFacetSize: Int = 50
  private val minFacetSize: Int = 0
  private val maxFacetSize: Int = 2000
  private val defaultOp: String = "AND"
  private val defaultPage: Int = 1
  private val minPage: Int = 1
  private val maxPage: Int = 1000
  private val defaultPageSize: Int = 10
  private val minPageSize: Int = 0
  private val maxPageSize: Int = 1000
  private val defaultSortOrder: String = "asc"

  // A user can give any of the following parameters in a search request.
  private val acceptedSearchParams: Seq[String] =
    searchableDplaFields ++ Seq(
      "exact_field_match",
      "facets",
      "facet_size",
      "fields",
      "op",
      "page",
      "page_size",
      "q",
      "sort_by",
      "sort_order"
    )

  final case class ValidationException(
                                        private val message: String = ""
                                      ) extends Exception(message)

  /**
   * Get valid fetch params.
   * Fails with ValidationException if id or any raw params are invalid.
   */
  private def getFetchId(id: String,
                         rawParams: Map[String, String]
                        ): Try[String] =
    Try {
      // There are no recognized params for a fetch request
      if (rawParams.nonEmpty)
        throw ValidationException(
          "Unrecognized parameter: " + rawParams.keys.mkString(", ")
        )
      else
        getValidId(id)
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
        // Collect all the user-submitted field filters.
        val filters: Seq[FieldFilter] =
          searchableDplaFields.flatMap(getValidFieldFilter(rawParams, _))

        // Return valid search params. Provide defaults when appropriate.
        SearchParams(
          exactFieldMatch =
            getValid(rawParams, "exact_field_match", validBoolean)
              .getOrElse(defaultExactFieldMatch),
          facets =
            getValid(rawParams, "facets", validFields),
          facetSize =
            getValid(rawParams, "facet_size", validInt)
              .getOrElse(defaultFacetSize),
          fields =
            getValid(rawParams, "fields", validFields),
          filters =
            filters,
          op =
            getValid(rawParams, "op", validAndOr)
              .getOrElse(defaultOp),
          page =
            getValid(rawParams, "page", validInt)
              .getOrElse(defaultPage),
          pageSize =
            getValid(rawParams, "page_size", validInt)
              .getOrElse(defaultPageSize),
          q =
            getValid(rawParams, "q", validText),
          sortBy =
            getValid(rawParams, "sort_by", validField),
          sortOrder =
            getValid(rawParams, "sort_order", validSortOrder)
              .getOrElse(defaultSortOrder)
        )
      }
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
   * Get a valid value for a field filter.
   */
  private def getValidFieldFilter(rawParams: Map[String, String],
                                  paramName: String): Option[FieldFilter] = {

    // Look up the parameter's field type.
    // Use this to determine the appropriate validation method.
    val validationMethod: (String, String) => String =
    getDplaFieldType(paramName) match {
      case Some(fieldType) =>
        fieldType match {
          case TextField => validText
          case URLField => validUrl
          case _ => validText // This should not happen
        }
      case None =>
        throw ValidationException(s"Unrecognized parameter: $paramName")
    }

    getValid(rawParams, paramName, validationMethod)
      .map(FieldFilter(paramName, _))
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

  // One field.
  // Must be in the list of accepted fields for the given param.
  private def validField(fieldString: String, param: String): String = {
    val acceptedFields = param match {
      case "sort_by" => sortableDplaFields
      case _ => Seq[String]()
    }

    if (acceptedFields.contains(fieldString))
      fieldString
    else
      throw ValidationException(
        s"'$fieldString' is not an allowable value for '$param'"
      )
  }

  // One or more fields.
  // Must be in the list of accepted fields for the given param.
  private def validFields(fieldString: String, param: String): Seq[String] = {
    val acceptedFields = param match {
      case "facets" => facetableDplaFields
      case "fields" => allDplaFields
      case _ => Seq[String]()
    }

    val filtered = fieldString.split(",").map(candidate => {
      if (acceptedFields.contains(candidate))
        candidate
      else
        throw ValidationException(
          s"'$candidate' is not an allowable value for '$param'"
        )
    })

    if (filtered.nonEmpty)
      filtered
    else
      throw ValidationException(
        s"$param must contain at least one valid field"
      )
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

  // Must be a string between 2 and 200 characters.
  private def validText(text: String, param: String): String =
    if (text.length < 2 || text.length > 200)
    // In the DPLA API (cultural heritage), an exception is thrown if q is too
    // long, but not if q is too short.
    // For internal consistency, and exception is thrown here in both cases.
      throw ValidationException(s"$param must be between 2 and 200 characters")
    else text

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