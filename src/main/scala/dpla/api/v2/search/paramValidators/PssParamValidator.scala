package dpla.api.v2.search.paramValidators

import dpla.api.v2.search.models.PssFields

import scala.util.{Failure, Success, Try}

object PssParamValidator extends ParamValidator with PssFields {

  // These parameters are valid for a search request.
  override protected val acceptedSearchParams: Seq[String] =
    searchableDataFields ++ Seq("fields", "filter", "sort_by", "sort_order")

  // These parameters are valid for a fetch request.
  override protected val acceptedFetchParams: Seq[String] = Seq()

  // These fields are not valid for search, sort, filter, & facets.
  // Rather than returning an error, they should be ignored.
  override protected val ignoredFields: Seq[String] = Seq()

  // Return all primary source sets by default.
  override protected val defaultPageSize: Int = 200

  override protected def preProcess(unprocessed: Map[String, String]): Map[String, String] = {
    if (unprocessed.keys.toSet.contains("id")) {
      // Get a single set
      unprocessed + ("fields" -> setFields.mkString(","))
    } else if (unprocessed.keys.toSet.contains("hasPart.id")) {
      // Get a source
      val shortId = unprocessed("hasPart.id")

      // Safety check that shortId is an Int
      Try { shortId.toInt } match {
        case Success(_) =>
          val longId = "\"https://api.dp.la/primary-source-sets/sources/" + shortId + "\""
          unprocessed - "hasPart.id" + ("hasPart.id" -> longId)
        case Failure(_) =>
          // This should cause an appropriate validation failure during normal
          // validation, which will check that hasPart.id is a valid URL
          unprocessed
      }

    } else {
      // Get multiple sets

      // Default to ordering by most recently added
      val sortBy: String = unprocessed.get("order") match {
        case Some(order) => order match {
          case "recently_added" => "dateCreated"
          case _ => "dateCreated"
        }
        case None => "dateCreated"
      }

      val sortOrder: String = unprocessed.get("order") match {
        case Some(order) => order match {
          case "recently_added" => "desc"
          case _ => "desc"
        }
        case None => "desc"
      }

      unprocessed + (
        "fields" -> allSetsFields.mkString(","),
        "sort_by" -> sortBy,
        "sort_order" -> sortOrder
      ) - ("order")
    }
  }

  val allSetsFields: Seq[String] = Seq(
    "@context",
    "@id",
    "@type",
    "about",
    "dateCreated",
    "name",
    "repImageUrl",
    "thumbnailUrl"
  )

  val setFields: Seq[String] = Seq(
    "@context",
    "@id",
    "@type",
    "dct:created",
    "dct:modified",
    "dct:type",
    "accessibilityControl",
    "accessibilityFeature",
    "accessibilityHazard",
    "about",
    "author",
    "dateCreated",
    "dateModified",
    "description",
    "educationalAlignment",
    "hasPart.@id",
    "hasPart.@type",
    "hasPart.author",
    "hasPart.disambiguatingDescription",
    "hasPart.hasPart",
    "hasPart.mainEntity.@type",
    "hasPart.name",
    "hasPart.repImageUrl",
    "hasPart.thumbnailUrl",
    "hasPart.text",
    "inLanguage",
    "isRelatedTo",
    "learningResourceType",
    "license",
    "name",
    "publisher",
    "repImageUrl",
    "thumbnailUrl",
    "typicalAgeRange"
  )
}
