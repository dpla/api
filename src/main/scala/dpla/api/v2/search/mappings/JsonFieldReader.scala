package dpla.api.v2.search.mappings

import spray.json._
import scala.annotation.tailrec

/**
 * Methods for reading JSON
 */
trait JsonFieldReader extends DefaultJsonProtocol {

  /** Public methods
   * These methods take a root JsObject and a path of zero to many children
   * They return the specified type of JsValue
   */

  def readObject(root: JsObject, path: String*): Option[JsObject] = {

    @tailrec
    def getNext(current: Option[JsObject], next: Seq[String]): Option[JsObject] =
      if (next.isEmpty) current
      else {
        current match {
          case Some(obj) => getNext(getObjectOpt(obj, next.head), next.drop(1))
          case _ => None
        }
      }

    getNext(Some(root), path)
  }

  def readObjectArray(root: JsObject, path: String*): Seq[JsObject] =
    read(getObjectSeq, root, path).getOrElse(Seq[JsObject]())

  def readString(root: JsObject, path: String*): Option[String] =
    read(getStringOpt, root, path)

  def readStringArray(root: JsObject, path: String*): Seq[String] =
    read(getStringSeq, root, path).getOrElse(Seq[String]())

  def readInt(root: JsObject, path: String*): Option[Int] =
    read(getIntOpt, root, path)

  def readBoolean(root: JsObject, path: String*): Option[Boolean] =
    read(getBooleanOpt, root, path)

  /**
   * Read a path with an unknown data type at the end.
   */
  def readUnknown(parent: JsObject, children: String*): Option[JsValue] = {

    val child = children.headOption
    val nextChildren = children.drop(1)

    child match {
      case Some(c) =>
        parent.getFields(c) match {
          case Seq(value: JsObject) =>
            if (nextChildren.isEmpty) Some(value)
            else readUnknown(value, nextChildren: _*)
          case Seq(JsString(value)) =>
            if (nextChildren.isEmpty) Some(value.toJson)
            else None
          case Seq(JsNumber(value)) =>
            if (nextChildren.isEmpty) Some(value.intValue.toJson)
            else None
          case Seq(JsBoolean(value)) =>
            if (nextChildren.isEmpty) Some(value.booleanValue.toJson)
            else None
          case Seq(JsArray(vector)) => Some(vector.flatMap(_ match {
            case JsString(value) =>
              if (nextChildren.isEmpty) Some(value.toJson)
              else None
            case JsObject(value) =>
              if (nextChildren.isEmpty) Some(JsObject(value).toJson)
              else readUnknown(value.toJson.asJsObject, nextChildren: _*)
            case _ => None
          })).map(vector => {
            // collapse arrays with a single value
            if (vector.length == 1) vector.headOption.toJson
            else vector.toJson
          })
          case _ => None
        }
      case None => None
    }
  }

  /** Private helper methods */

  private def read[T](getMethod: (JsObject, String) => Option[T],
                      root: JsObject,
                      path: Seq[String]): Option[T] = {

    val nestedObjects: Seq[String] = path.dropRight(1)
    val lastField: Option[String] = path.lastOption

    readObject(root, nestedObjects: _*) match {
      case Some(parent) => lastField match {
        case Some(child) => getMethod(parent, child)
        case _ => None // no children were provided in method parameters
      }
      case _ => None // parent object not found
    }
  }

  /**
   * These methods take a parent JsObject and a single child
   * They return the specified type of JsObject
   */

  private def getObjectOpt(parent: JsObject, child: String): Option[JsObject] = {
    parent.getFields(child) match {
      case Seq(value: JsObject) => Some(value)
      case _ => None
    }
  }

  private def getObjectSeq(parent: JsObject, child: String): Option[Seq[JsObject]] =
    parent.getFields(child) match {
      case Seq(JsArray(vector)) => Some(vector.flatMap(_ match {
        case JsObject(value) => Some(JsObject(value))
        case _ => None
      }))
      case Seq(JsObject(value)) => Some(Seq(JsObject(value)))
      case _ => None
    }


  private def getStringOpt(parent: JsObject, child: String): Option[String] =
    parent.getFields(child) match {
      case Seq(JsString(value)) => Some(value)
      case Seq(JsNumber(value)) => Some(value.toString)
      case _ => None
    }

  private def getStringSeq(parent: JsObject, child: String): Option[Seq[String]] =
    parent.getFields(child) match {
      case Seq(JsArray(vector)) => Some(vector.flatMap(_ match {
        case JsString(value) => Some(value)
        case _ => None
      }))
      case Seq(JsString(value)) => Some(Seq(value))
      case _ => None
    }

  private def getIntOpt(parent: JsObject, child: String): Option[Int] =
    parent.getFields(child) match {
      case Seq(JsNumber(value)) => Some(value.intValue)
      case _ => None
    }

  private def getBooleanOpt(parent: JsObject, child: String): Option[Boolean] =
    parent.getFields(child) match {
      case Seq(JsBoolean(value)) => Some(value.booleanValue)
      case _ => None
    }
}
