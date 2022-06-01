package dpla.api.v2.search

import spray.json._
import dpla.api.v2.search.JsonFormats._

import scala.annotation.tailrec


// Case class for reading JSON of an unknown type
case class JsonValue(
                      value: JsValue,
                      `type`: JsonValueType,
                      path: String
                    )

sealed trait JsonValueType
case object ObjectType extends JsonValueType
case object ObjectSeqType extends JsonValueType
case object StringType extends JsonValueType
case object StringSeqType extends JsonValueType
case object IntType extends JsonValueType
case object BooleanType extends JsonValueType
case object NullType extends JsonValueType


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

  def readUnknown(parent: JsObject, children: String*): Option[JsValue] = {

    val child = children.headOption
    val nextChildren = children.drop(1)

    child match {
      case Some(c) =>
        parent.getFields(c) match {
          case Seq(value: JsObject) =>
            if (nextChildren.isEmpty) Some(value)
            else readUnknown(value, nextChildren:_*)
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
              else readUnknown(value.toJson.asJsObject, nextChildren:_*)
            case _ => None
          })).map(_.toJson)
          case _ => None
        }
      case None => None
    }
  }

  /**
   * Read a path with an unknown data type at the end.
   * @return Option[Any] or Seq[Any]
   *
   * TODO: getValue can read a path unless there are internal arrays
   * Still trying to figure out how to deal with internal array(s).
   */
//  private def readUnknownOld(root: JsObject, path: String*): IterableOnce[Any] = {
//
//    val methods:  Seq[(JsObject, Seq[String]) => IterableOnce[Any]] =
//      Seq(
//        readObject,
//        readObjectArray,
//        readString,
//        readStringArray,
//        readInt,
//        readBoolean
//      )
//
//    @ tailrec
//    def getValue(pathToRead: Seq[String],
//                 currentMethod: (JsObject, Seq[String]) => IterableOnce[Any],
//                 nextMethod: Seq[(JsObject, Seq[String]) => IterableOnce[Any]]
//                ): IterableOnce[Any] = {
//
//      // Try to read the path using the current method
//      val result: IterableOnce[Any] = currentMethod(root, pathToRead)
//
//      if (nextMethod.isEmpty) result
//      else result match {
//        case Some(_) =>
//          result
//        case x: Seq[Any] =>
//          if (x.nonEmpty) result
//          else getValue(pathToRead, nextMethod.head, nextMethod.drop(1))
//        case _ =>
//          getValue(pathToRead, nextMethod.head, nextMethod.drop(1))
//      }
//    }
//
//    // Parse the value into JSON and find it's data type
//    // TODO roll this into getValue
//    def parseValue(value: IterableOnce[Any], path: Seq[String]): JsonValue = {
//      val pathStr = path.mkString(".")
//
//      value match {
//        case Some(x: JsObject) =>
//          JsonValue(x.toJson, ObjectType, pathStr)
//        case Some(x: String) =>
//          JsonValue(x.toJson, StringType, pathStr)
//        case Some(x: Int) =>
//          JsonValue(x.toJson, IntType, pathStr)
//        case Some(x: Boolean) =>
//          JsonValue(x.toJson, BooleanType, pathStr)
//        case x: Seq[_] => x.headOption match {
//          case Some(_: JsObject) =>
//            JsonValue(x.map(_.asInstanceOf[JsObject]).toJson, ObjectSeqType, pathStr)
//          case Some(_: String) =>
//            JsonValue(x.map(_.asInstanceOf[String]).toJson, StringSeqType, pathStr)
//          case _ =>
//            JsonValue(null, NullType, pathStr)
//        }
//        case None => JsonValue(null, NullType, pathStr)
//        case _ => JsonValue(null, NullType, pathStr)
//      }
//    }
//
//    def readPath(currentPath: Seq[String], nextPath: Seq[String]): Seq[JsonValue] ={
//      val rawValue = getValue(currentPath, methods.head, methods.drop(1))
//      val value = parseValue(rawValue, currentPath)
//
//      if (value.`type` != ObjectType && value.`type` != ObjectSeqType) {
//        value
//      } else if (nextPath.isEmpty) {
//        value
//      } else if (value.`type` == ObjectType) {
//        readPath(currentPath :+ nextPath.head, nextPath.drop(1))
//      } else {
//        // value.`type` == ObjectSeqType
//        rawValue.iterator.zipWithIndex.flatMap { case (obj, index) =>
//          val root = obj.asInstanceOf[JsObject]
//          readUnknown(root, nextPath.drop(1): _*)
//        }.toSeq
//      }
//
//    }
////      if (nextPath.isEmpty) {
////        getValue(currentPath, methods.head, methods.drop(1))
////      } else {
////        getValue(currentPath, methods.head, methods.drop(1)) match {
////          case Some(_: JsObject) =>
////            readPath(currentPath :+ nextPath.head, nextPath.drop(1))
////          case Some(seq: Seq[_]) =>
////            seq.head match {
////              case JsObject =>
////                seq.map(_ => readPath(currentPath :+ nextPath.head, nextPath.drop(1)))
////              case _ =>
////                throw new RuntimeException("Cannot parse field path.")
////            }
////          case _ =>
////            throw new RuntimeException("Cannot parse field path.")
////        }
////      }
//
//    readPath(path.take(1), path.drop(1))
//  }

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
