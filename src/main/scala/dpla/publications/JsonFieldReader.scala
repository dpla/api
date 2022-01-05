package dpla.publications

import spray.json.{JsArray, JsNumber, JsObject, JsString}

import scala.annotation.tailrec

/** Methods for reading JSON **/
trait JsonFieldReader {

  def readString(root: JsObject, path: String*): Option[String] = {
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

  def readStringArray(root: JsObject, path: String*): Seq[String] = {
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

  def readInt(root: JsObject, path: String*): Option[Int] = {
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

  private def getNestedObject(root: JsObject, children: Seq[String]): Option[JsObject] = {

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

  private def getObjOpt(parent: JsObject, child: String): Option[JsObject] = {
    parent.getFields(child) match {
      case Seq(value: JsObject) => Some(value)
      case _ => None
    }
  }

  private def getStringOpt(parent: JsObject, child: String): Option[String] =
    parent.getFields(child) match {
      case Seq(JsString(value)) => Some(value)
      case Seq(JsNumber(value)) => Some(value.toString)
      case _ => None
    }

  private def getStringSeq(parent: JsObject, child: String): Seq[String] =
    parent.getFields(child) match {
      case Seq(JsArray(vector)) => vector.flatMap(_ match {
        case JsString(value) => Some(value)
        case _ => None
      })
      case Seq(JsString(value)) => Seq(value)
      case _ => Seq[String]()
    }

  private def getIntOpt(parent: JsObject, child: String): Option[Int] =
    parent.getFields(child) match {
      case Seq(JsNumber(value)) => Some(value.intValue)
      case _ => None
    }
}
