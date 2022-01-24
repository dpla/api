package dpla.ebookapi.helpers

import scala.io.{BufferedSource, Source}

trait FileReader {
  def readFile(filePath: String): String = {
    val source: String = getClass.getResource(filePath).getPath
    val buffered: BufferedSource = Source.fromFile(source)
    buffered.getLines.mkString
  }
}
