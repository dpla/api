package dpla.ebookapi.v1.ebooks





final case class ValidationException(private val message: String = "") extends Exception(message)




