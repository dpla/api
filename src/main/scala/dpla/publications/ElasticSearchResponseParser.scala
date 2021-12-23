package dpla.publications

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._
import spray.json.DefaultJsonProtocol._

object ElasticSearchResponseParser {

//  type FromEntityUnmarshaller[Publications] = Unmarshaller[HttpEntity, Publications]

  def parseAll(responseEntity: ResponseEntity) = {

    val body = responseEntity.toString


    // JSON parse to Publications case class
  }
}

case class ESDocList()

case class ESDoc()


