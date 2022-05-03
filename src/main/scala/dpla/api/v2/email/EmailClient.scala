package dpla.api.v2.email

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, DispatcherSelector}
import com.amazonaws.services.simpleemail.model._
import com.amazonaws.services.simpleemail.{AmazonSimpleEmailService, AmazonSimpleEmailServiceClientBuilder}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

sealed trait EmailClientResponse
case object EmailSuccess extends EmailClientResponse
case object EmailFailure extends EmailClientResponse

/**
 * Handles sending emails.
 */
object EmailClient {

  sealed trait EmailClientCommand

  final case class SendEmail(
                              addressee: String,
                              subject: String,
                              content: String,
                              replyTo: ActorRef[EmailClientResponse]
                            ) extends EmailClientCommand

  private final case class ProcessEmailResponse(
                                                 response: Try[SendEmailResult],
                                                 replyTo: ActorRef[EmailClientResponse]
                                               ) extends EmailClientCommand

  private final case class ReturnFinalResponse(
                                                response: EmailClientResponse,
                                                replyTo: ActorRef[EmailClientResponse],
                                                error: Option[Throwable] = None
                                              ) extends EmailClientCommand

  def apply(): Behavior[EmailClientCommand] = {

   Behaviors.setup { context =>

     val emailFrom: String = context.system.settings.config
       .getString("awsSes.emailFrom")

     val awsRegion: String = context.system.settings.config
       .getString("awsSes.region")

     val awsClient: AmazonSimpleEmailService =
       AmazonSimpleEmailServiceClientBuilder.standard()
        .withRegion(awsRegion)
        .build

     Behaviors.receiveMessage[EmailClientCommand] {

       case SendEmail(addressee, subject, content, replyTo) =>
         val request = new SendEmailRequest()
           .withDestination(new Destination()
             .withToAddresses(addressee)
           )
           .withMessage(new Message()
             .withBody(new Body()
               .withText(new Content()
                 .withCharset("UTF-8")
                 .withData(content)
               )
             )
             .withSubject(new Content()
               .withCharset("UTF-8")
               .withData(subject)
             )
           )
           .withSource(emailFrom)

         // The SES SDK can only create an async request that returns a Java
         // future, which cannot be converted to Scala.
         // Therefore, we're employing a synchronous request that will run on
         // a dedicated dispatcher with a fixed thread pool.
         // See https://doc.akka.io/docs/akka/current/typed/dispatchers.html#blocking-needs-careful-management
         val blockingExecutionContext: ExecutionContext =
           context.system.dispatchers.lookup(
             DispatcherSelector.fromConfig("dispatchers.emailDispatcher")
           )

         // Create a future response.
         val responseFuture: Future[Try[SendEmailResult]] =
           Future{
             Try{
               // The blocking operation.
               awsClient.sendEmail(request)
             }
           }(blockingExecutionContext)

         // Map the Future value to a message, handled by this actor.
         context.pipeToSelf(responseFuture) {
           case Success(response) =>
             ProcessEmailResponse(response, replyTo)
           case Failure(e) =>
             ReturnFinalResponse(EmailFailure, replyTo, Some(e))
         }
         Behaviors.same

       case ProcessEmailResponse(response, replyTo) =>
         response match {
           case Success(_) =>
             replyTo ! EmailSuccess
           case Failure(e) =>
             context.log.error("Email send failure:", e)
             replyTo ! EmailFailure
         }
         Behaviors.same

       case ReturnFinalResponse(response, replyTo, error) =>
         error match {
           case Some(e) =>
             context.log.error(
               "Failed to process a Future", e
             )
           case None => // no-op
         }
         replyTo ! response
         Behaviors.same
     }
   }
  }
}
