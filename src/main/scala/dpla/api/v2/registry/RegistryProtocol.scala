package dpla.api.v2.registry

object RegistryProtocol {

  /**
   * Responses that are share among registries.
   * Individual registries may have additional responses of their own.
   */
  trait RegistryResponse

  final case class ValidationFailure(message: String) extends RegistryResponse
  final case object ForbiddenFailure extends RegistryResponse
  final case object NotFoundFailure extends RegistryResponse
  final case object InternalFailure extends RegistryResponse
}
