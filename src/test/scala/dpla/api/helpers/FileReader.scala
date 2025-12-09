package dpla.api.helpers

import scala.io.{BufferedSource, Source}

trait FileReader {
  def readFile(filePath: String): String = {
    val source: String = getClass.getResource(filePath).getPath
    val buffered: BufferedSource = Source.fromFile(source, "UTF-8")
    try buffered.getLines().mkString
    finally buffered.close()
  }
}
