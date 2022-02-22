package dpla.ebookapi.v1

trait RegistryResponse

final case class ValidationFailure(message: String) extends RegistryResponse
case object ForbiddenFailure extends RegistryResponse
case object NotFoundFailure extends RegistryResponse
case object InternalFailure extends RegistryResponse
