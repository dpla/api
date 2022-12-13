package dpla.api.helpers

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import dpla.api.v2.search.mappings.JsonFieldReader
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json._

import scala.util.Try

trait ITHelper extends AnyWordSpec with Matchers with ScalatestRouteTest
  with JsonFieldReader {

  val routes: Route

  def returnStatusCode(code: Int)(implicit request: HttpRequest): Unit =
    s"return status code $code" in {
      request ~> routes ~> check {
        status.intValue shouldEqual code
      }
    }

  def returnJSON(implicit request: HttpRequest): Unit =
    "return JSON" in {
      request ~> routes ~> check {
        val parsed = Try {
          entityAs[String].parseJson
        }.toOption
        parsed shouldNot be(None)
      }
    }

  def includeField(field: String)(implicit request: HttpRequest): Unit =
    s"include field $field" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val path = field.split("\\.")
        val value: Option[JsValue] = readUnknown(entity, path:_*)
        value shouldNot be(None)
      }
    }

  def returnArrayWithSize(field: String, size: Int)(implicit request: HttpRequest): Unit =
    s"return $field array with size $size" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject

        val docs = readObjectArray(entity, field)
        docs.size should ===(size)
      }
    }

  def returnInt(field: String, int: Int)(implicit request: HttpRequest): Unit =
    s"return $field $int" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val count: Option[Int] = readInt(entity, field)
        count should contain(int)
      }
    }

  def returnString(field: String, string: String)(implicit request: HttpRequest): Unit =
    s"return $field $string" in {
      request ~> routes ~> check {
        val entity: JsObject = entityAs[String].parseJson.asJsObject
        val path = field.split("\\.")
        val count: Option[String] = readString(entity, path:_*)
        count should contain(string)
      }
    }
}