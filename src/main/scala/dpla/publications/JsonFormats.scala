package dpla.publications

import spray.json.DefaultJsonProtocol
import spray.json.{JsString, JsValue, RootJsonFormat, _}

import scala.annotation.tailrec

object JsonFormats extends DefaultJsonProtocol {

  implicit object PublicationFormat extends RootJsonFormat[Publication] {

    def read(json: JsValue): Publication = {
      val root: JsObject = json.asJsObject()

      Publication(
        author = parseStringArray(root, "_source", "author"),
        genre = parseStringArray(root, "_source", "genre"),
        id = parseString(root, "_id"),
        itemUri = parseString(root, "_source", "itemUri"),
        language = parseStringArray(root, "_source", "language"),
        payloadUri = parseStringArray(root, "_source", "payloadUri"),
        sourceUri = parseString(root, "_source", "sourceUri"),
        summary = parseStringArray(root, "_source", "summary"),
        title = parseStringArray(root, "_source", "title")
      )
    }

    def write(pub: Publication): JsValue =
      JsObject(
        "count" -> JsNumber(1),
        "docs" -> JsObject(
          "id" -> pub.id.toJson,
          "dataProvider" -> pub.sourceUri.toJson,
          "ingestType" -> JsString("ebook"),
          "isShownAt" -> pub.itemUri.toJson,
          "object" -> pub.payloadUri.toJson,
          "sourceResource" -> JsObject(
            "creator" -> pub.author.toJson,
            "description" -> pub.summary.toJson,
            "language" -> JsObject(
              "name" -> pub.language.toJson,
            ),
            "title" -> pub.title.toJson,
            "subject" -> JsObject(
              "name" -> pub.genre.toJson
            )
          )
        )
      ).toJson
  }

  implicit object PublicationsFormat extends RootJsonFormat[Publications] {

    def read(json: JsValue): Publications = {

      val root = json.asJsObject

      Publications(
        count = parseInt(root, "hits", "total", "value"),
        limit = 0,
        start = 0
      )
    }

    def write(pl: Publications): JsValue =
      JsObject(
        "count" -> pl.count.toJson,
        "start" -> pl.start.toJson,
        "limit" -> pl.limit.toJson
      ).toJson
  }

  // Methods for parsing JSON

  def parseString(root: JsObject, path: String*): Option[String] = {
    val nestedObjects: Seq[String] = path.dropRight(1)
    val lastField: Option[String] = path.lastOption

    getNestedObject(root, nestedObjects) match {
      case Some(parent) => lastField match {
        case Some(child) => getStringOpt(parent, child)
        case _ => None // no children were provided in method parameters
      }
      case _ => None // parent object not found
    }
  }

  def parseStringArray(root: JsObject, path: String*): Seq[String] = {
    val nestedObjects: Seq[String] = path.dropRight(1)
    val lastField: Option[String] = path.lastOption

    getNestedObject(root, nestedObjects) match {
      case Some(parent) => lastField match {
        case Some(child) => getStringSeq(parent, child)
        case _ => Seq[String]() // no children were provided in method parameters
      }
      case _ => Seq[String]() // parent object not found
    }
  }

  def parseInt(root: JsObject, path: String*): Option[Int] = {
    val nestedObjects: Seq[String] = path.dropRight(1)
    val lastField: Option[String] = path.lastOption

    getNestedObject(root, nestedObjects) match {
      case Some(parent) => lastField match {
        case Some(child) => getIntOpt(parent, child)
        case _ => None // no children were provided in method parameters
      }
      case _ => None // parent object not found
    }
  }

  def getNestedObject(root: JsObject, children: Seq[String]): Option[JsObject] = {

    @tailrec
    def getNext(current: Option[JsObject], next: Seq[String]): Option[JsObject] =
      if (next.isEmpty) current
      else {
        current match {
          case Some(obj) => getNext(getObjOpt(obj, next.head), next.drop(1))
          case _ => None
        }
      }

    getNext(Some(root), children)
  }

  def getObjOpt(parent: JsObject, child: String): Option[JsObject] = {
    parent.getFields(child) match {
      case Seq(value: JsObject) => Some(value)
      case _ => None
    }
  }

  def getStringOpt(parent: JsObject, child: String): Option[String] =
    parent.getFields(child) match {
      case Seq(JsString(value)) => Some(value)
      case Seq(JsNumber(value)) => Some(value.toString)
      case _ => None
    }

  def getStringSeq(parent: JsObject, child: String): Seq[String] =
    parent.getFields(child) match {
      case Seq(JsArray(vector)) => vector.flatMap(_ match {
        case JsString(value) => Some(value)
        case _ => None
      })
      case Seq(JsString(value)) => Seq(value)
      case _ => Seq[String]()
    }

  def getIntOpt(parent: JsObject, child: String): Option[Int] =
    parent.getFields(child) match {
      case Seq(JsNumber(value)) => Some(value.intValue)
      case _ => None
    }
}

// Case classes

case class Publications(
                         count: Option[Int],
                         limit: Int,
                         start: Int
                         //                             docs: Array[Publication]
                       )

case class Publication(
                        author: Seq[String],
                        genre: Seq[String],
                        id: Option[String],
                        itemUri: Option[String],
                        language: Seq[String],
                        payloadUri: Seq[String],
                        sourceUri: Option[String],
                        summary: Seq[String],
                        title: Seq[String]
                      )
