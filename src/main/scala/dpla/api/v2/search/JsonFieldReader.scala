package dpla.api.v2.search

import spray.json.{JsArray, JsBoolean, JsNumber, JsObject, JsString, deserializationError}

import scala.annotation.tailrec


// Case class for reading JSON of an unknown type
case class JsonValue(
                      value: IterableOnce[Any],
                      `type`: JsonValueType
                    )

sealed trait JsonValueType
case object ObjectOpt extends JsonValueType
case object ObjectSeq extends JsonValueType
case object StringOpt extends JsonValueType
case object StringSeq extends JsonValueType
case object IntOpt extends JsonValueType
case object BooleanOpt extends JsonValueType


/**
 * Methods for reading JSON
 */
trait JsonFieldReader {

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
   * @return Option[Any] or Seq[Any]
   *
   * TODO: getValue can read a path unless there are internal arrays
   * Still trying to figure out how to deal with internal array(s).
   */
  def readUnknown(root: JsObject, path: String*): IterableOnce[Any] = {

    val methods:  Seq[(JsObject, Seq[String]) => IterableOnce[Any]] =
      Seq(
        readObject,
        readObjectArray,
        readString,
        readStringArray,
        readInt,
        readBoolean
      )

    @ tailrec
    def getValue(pathToRead: Seq[String],
                 currentMethod: (JsObject, Seq[String]) => IterableOnce[Any],
                 nextMethod: Seq[(JsObject, Seq[String]) => IterableOnce[Any]]
                ): IterableOnce[Any] = {

      // Try to read the path using the current method
      val result: IterableOnce[Any] = currentMethod(root, pathToRead)

      if (nextMethod.isEmpty) result
      else result match {
        case Some(_) =>
          result
        case x: Seq[Any] =>
          if (x.nonEmpty) result
          else getValue(pathToRead, nextMethod.head, nextMethod.drop(1))
        case _ =>
          getValue(pathToRead, nextMethod.head, nextMethod.drop(1))
      }
    }

    def readPath(currentPath: Seq[String], nextPath: Seq[String]): IterableOnce[Any] =
      if (nextPath.isEmpty) {
        getValue(currentPath, methods.head, methods.drop(1))
      } else {
        getValue(currentPath, methods.head, methods.drop(1)) match {
          case Some(_: JsObject) =>
            readPath(currentPath :+ nextPath.head, nextPath.drop(1))
          case Some(seq: Seq[_]) =>
            seq.head match {
              case JsObject =>
                seq.map(_ => readPath(currentPath :+ nextPath.head, nextPath.drop(1)))
              case _ =>
                throw new RuntimeException("Cannot parse field path.")
            }
          case _ =>
            throw new RuntimeException("Cannot parse field path.")
        }
      }

    readPath(path.take(1), path.drop(1))
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
      case _ => None
    }

  private def getStringSeq(parent: JsObject, child: String): Option[Seq[String]] =
    parent.getFields(child) match {
      case Seq(JsArray(vector)) => Some(vector.flatMap(_ match {
        case JsString(value) => Some(value)
        case _ => None
      }))
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
