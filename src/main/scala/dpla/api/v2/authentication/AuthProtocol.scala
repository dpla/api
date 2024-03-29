package dpla.api.v2.authentication

import akka.actor.typed.ActorRef

object AuthProtocol {

  /** Public command protocol */
  sealed trait AuthenticationCommand

  case class FindAccountByKey(
                               apiKey: Option[String],
                               replyTo: ActorRef[AuthenticationResponse]
                             ) extends AuthenticationCommand

  case class CreateAccount(
                            email: String,
                            replyTo: ActorRef[AuthenticationResponse]
                          ) extends AuthenticationCommand

  /** Public response protocol */
  sealed trait AuthenticationResponse

  final case class AccountCreated(account: Account) extends AuthenticationResponse
  final case class AccountFound(account: Account) extends AuthenticationResponse
  final case object AccountNotFound extends AuthenticationResponse
  final case object InvalidApiKey extends AuthenticationResponse
  final case object InvalidEmail extends AuthenticationResponse
  case object AuthenticationFailure extends AuthenticationResponse

  /**
   * Internal command protocol.
   * Used by actors within the search package to communicate with one another.
   */
  private[authentication] trait IntermediateAuthResult

  private[authentication] final case class RawApiKey(
                                                      apiKey: String,
                                                      replyTo: ActorRef[AuthenticationResponse]
                                                    ) extends IntermediateAuthResult

  private[authentication] final case class RawEmail(
                                                     email: String,
                                                     replyTo: ActorRef[AuthenticationResponse]
                                                   ) extends IntermediateAuthResult

  private[authentication] final case class ValidApiKey(

                                                        apiKey: String,
                                                        replyTo: ActorRef[AuthenticationResponse]
                                                      ) extends IntermediateAuthResult

  private[authentication] final case class ValidEmail(
                                                       email: String,
                                                       replyTo: ActorRef[AuthenticationResponse]
                                                     ) extends IntermediateAuthResult
}
