package dpla.ebookapi.v1.ebooks

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}

import java.net.URL
import scala.util.{Failure, Success, Try}
import org.apache.commons.validator
import org.apache.commons.validator.routines.EmailValidator

/**
 * Validates user-submitted parameters. Provides default values when appropriate.
 * Bad actors may use invalid search params to try and hack the system, so they
 * are logged as warnings.
 */

sealed trait ValidationResponse

final case class ValidSearchParams(
                                    apiKey: String,
                                    searchParams: SearchParams
                                  ) extends ValidationResponse

final case class ValidFetchParams(
                                   apiKey: String,
                                   fetchParams: FetchParams
                                 ) extends ValidationResponse

final case class ValidEmail(
                             email: String
                           ) extends ValidationResponse

final case class InvalidParams(
                                message: String
                              ) extends ValidationResponse

case object InvalidApiKey extends ValidationResponse

case class SearchParams(
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

case class FetchParams(
                        id: String
                      )

case class FieldFilter(
                        fieldName: String,
                        value: String
                      )

object ParamValidator extends DplaMapFields {

  sealed trait ValidationCommand

  final case class ValidateSearchParams(
                                         params: Map[String, String],
                                         replyTo: ActorRef[ValidationResponse]
                                       ) extends ValidationCommand

  final case class ValidateFetchParams(
                                        id: String,
                                        params: Map[String, String],
                                        replyTo: ActorRef[ValidationResponse]
                                      ) extends ValidationCommand

  final case class ValidateEmail(
                                  email: String,
                                  replyTo: ActorRef[ValidationResponse]
                                ) extends ValidationCommand

  def apply(): Behavior[ValidationCommand] = {
    Behaviors.setup { context =>
      Behaviors.receiveMessage {

        case ValidateSearchParams(params, replyTo) =>
          val response: ValidationResponse = getSearchParams(params)

          // Log warnings for invalid params
          response match {
            case InvalidParams(msg) =>
              context.log.warn2(
                "Invalid search params: '{}' for params '{}'",
                msg,
                params.map { case(key, value) => s"$key: $value"}.mkString(", ")
              )
            case InvalidApiKey =>
              context.log.warn(
                "Invalid format for API key: {}",
                params.getOrElse("api_key", "")
              )
            case _ => //noop
          }

          replyTo ! response
          Behaviors.same

        case ValidateFetchParams(id, params, replyTo) =>
          val response = getFetchParams(id, params)

          // Log warnings for invalid params
          response match {
            case InvalidParams(msg) =>
              context.log.warn2(
                "Invalid fetch params: '{}' for params '{}'",
                msg,
                params.map { case(key, value) => s"$key: $value"}.mkString(", ")
              )
            case InvalidApiKey =>
              context.log.warn(
                "Invalid format for API key: {}",
                params.getOrElse("api_key", "")
              )
            case _ => //noop
          }

          replyTo ! response
          Behaviors.same

        case ValidateEmail(email, replyTo) =>
          val response = getValidEmail(email)

          // Log warning for invalid params.
          response match {
            case InvalidParams(msg) =>
              context.log.warn(msg)

            case _ => // noop
          }

          replyTo ! response
          Behaviors.same
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
      "api_key",
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

  // A user can give any of the following parameters in a fetch request.
  private val acceptedFetchParams: Seq[String] =
    Seq("api_key")

  final case class ValidationException(
                                        private val message: String = ""
                                      ) extends Exception(message)

  private def getFetchParams(id: String,
                             rawParams: Map[String, String]): ValidationResponse = {

    // Check for unrecognized params
    val unrecognized = rawParams.keys.toSeq diff acceptedFetchParams

    if (unrecognized.nonEmpty)
      InvalidParams("Unrecognized parameter: " + rawParams.keys.mkString(", "))
    else {
      // Check for valid API Key
      getValidApiKey(rawParams) match {
        case Some(apiKey) =>
          // Check for valid ID
          Try{ getValidId(id) } match {
            case Success(id) =>
              ValidFetchParams(apiKey, FetchParams(id))
            case Failure(e) =>
              InvalidParams(e.getMessage)
        }
        case None => InvalidApiKey
      }
    }
  }

  private def getSearchParams(rawParams: Map[String, String]): ValidationResponse = {
    // Check for unrecognized params
    val unrecognized = rawParams.keys.toSeq diff acceptedSearchParams

    if (unrecognized.nonEmpty)
      InvalidParams("Unrecognized parameter: " + unrecognized.mkString(", "))
    else {
      // Check for valid API key
      getValidApiKey(rawParams) match {
        case Some(apiKey) =>
          // Check for valid search params
          Try {
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
          } match {
            case Success(searchParams) =>
              ValidSearchParams(apiKey, searchParams)
            case Failure(e) =>
              InvalidParams(e.getMessage)
          }
        case None => InvalidApiKey
      }
    }
  }

  /**
   * Validates email format using the Apache Commons validator.
   * Disallows pipe character (|) and single quote ('), both of which
   * could be used for SQL injection.
   * Limits length to 100 characters to be in compliance with database.
   */
  private def getValidEmail(email: String): ValidationResponse =
    if (EmailValidator.getInstance.isValid(email)
      && !email.contains("|")
      && !email.contains("'")
      && email.length <= 100)

      ValidEmail(email)
    else
      InvalidParams(s"$email is not a valid email address.")

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
   * Method returns valid API key, or None if API key is invalid.
   */
  private def getValidApiKey(rawParams: Map[String, String]): Option[String] = {
    Try {
      getValid(rawParams, "api_key", validApiKey)
    } match {
      case Success(keyOption) => keyOption
      case Failure(_) => None
    }
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

  // Must be a String 32 characters long, comprised of letters and numbers
  private def validApiKey(apiKey: String, param: String): String = {
    val rule = s"$param must be a 32 characters, and can only contain numbers" +
     "and letters"

    if (apiKey.length != 32) throw ValidationException(rule)
    else if (apiKey.matches("[a-zA-Z0-9-]*")) apiKey
    else throw ValidationException(rule)
  }
}
