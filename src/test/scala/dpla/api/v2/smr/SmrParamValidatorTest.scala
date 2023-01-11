package dpla.api.v2.smr

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import dpla.api.v2.smr.SmrProtocol._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec


class SmrParamValidatorTest extends AnyWordSpec with Matchers
  with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  val replyProbe: TestProbe[SmrResponse] =
    testKit.createTestProbe[SmrResponse]

  val interProbe: TestProbe[IntermediateSmrResult] =
    testKit.createTestProbe[IntermediateSmrResult]

  val smrParamValidator: ActorRef[IntermediateSmrResult] =
    testKit.spawn(SmrParamValidator(interProbe.ref))

  "smr param validator" should {
    "reject unrecognized params" in {
      val params = Map("foo" -> "bar")
      smrParamValidator ! RawParams(params, replyProbe.ref)
      replyProbe.expectMessageType[InvalidSmrParams]
    }
  }
}