package dpla.ebookapi.v1.registry

object RegistryProtocol {

  /**
   * Responses that are share among registries.
   * Individual registries may have additional responses of their own.
   * */
  trait RegistryResponse

  final case class ValidationFailure(message: String) extends RegistryResponse
  case object ForbiddenFailure extends RegistryResponse
  case object NotFoundFailure extends RegistryResponse
  case object InternalFailure extends RegistryResponse
}
