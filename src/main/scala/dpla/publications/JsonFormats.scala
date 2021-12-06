package dpla.publications

import com.example.UserRegistry.ActionPerformed

//#json-formats
import spray.json.DefaultJsonProtocol

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val publicationJsonFormat = jsonFormat2(Publication)
  implicit val publicationsJsonFormat = jsonFormat1(Publications)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
//#json-formats
