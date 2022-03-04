package dpla.ebookapi.v1

trait ValidationResponse

final case class InvalidParams(
                                message: String
                              ) extends ValidationResponse
