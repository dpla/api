package dpla.publications

import akka.actor.typed.ActorSystem
import akka.stream.ActorMaterializer
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}
import spray.json._
import JsonFormats._
import akka.NotUsed
import akka.util.ByteString
import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.common.JsonEntityStreamingSupport
import akka.stream.scaladsl.Source
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{ Directives, Route }
import akka.stream.ActorMaterializer
import spray.json.{ DefaultJsonProtocol, JsObject }

object ElasticSearchClient {

  // TODO move to environmental variable
  val elasticSearchEndpoint = "http://localhost:9200/eleanor/"

  def all: Future[Either[StatusCode, Future[Publications]]] = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
    // needed for the future map/onComplete
    implicit val executionContext: ExecutionContextExecutor = system.executionContext
    implicit val materializer = ActorMaterializer
    import system.dispatchers


    implicit val jsonStreamingSupport: JsonEntityStreamingSupport =
      EntityStreamingSupport.json()

    val uri = s"$elasticSearchEndpoint/_search?q=*"

    System.out.println(uri)

    val response: Future[HttpResponse] =
      Http().singleRequest(HttpRequest(uri = uri))

    response.map(res => {
      res.status.intValue match {
        case 200 => {

//          Unmarshal(response).to[Source[Publications, NotUsed]]

          val body: Future[String] = Unmarshaller.stringUnmarshaller(res.entity)

          val pubs: Future[Publications] = body.map(_.parseJson.convertTo[Publications])

//          res.entity.dataBytes
//            .via(jsonStreamingSupport.framingDecoder)
//            .mapAsync(bytes => Unmarshal(bytes).to[Publications])

//          //          import system.dispatcher
//          import scala.concurrent.duration._
//          val timeout = 300.millis
//
//          val bs: Future[ByteString] = res.entity.toStrict(timeout).map {
//            _.data
//          }
//          val s: Future[String] = bs.map(_.utf8String) // if you indeed need a `String`
//
//          System.out.println(s)
//
//          val pubs = s.map(_.parseJson.convertTo[Publications])
          Right(pubs)

        }
        case _ => {
          Left(res.status)
        }

      }
    })
  }

  def find(id: String): Future[Either[StatusCode, Publication]] = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
    // needed for the future map/onComplete
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    val response: Future[HttpResponse] =
      Http().singleRequest(HttpRequest(uri = s"$elasticSearchEndpoint/_doc/$id"))

    response.map(res => {
      res.status.intValue match {
        case 200 => Right(res.entity.toString.parseJson.convertTo[Publication])
        case _ => Left(res.status)
      }
    })
  }
}

case class ElasticSearchError(
                             code: Int,
                             message: String
                             )