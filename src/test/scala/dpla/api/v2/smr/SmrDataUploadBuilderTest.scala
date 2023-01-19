package dpla.api.v2.smr

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import akka.http.scaladsl.model.DateTime
import dpla.api.v2.smr.SmrProtocol._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec


class SmrDataUploadBuilderTest extends AnyWordSpec with Matchers
  with BeforeAndAfterAll {

  lazy val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  val replyProbe: TestProbe[SmrResponse] =
    testKit.createTestProbe[SmrResponse]

  val interProbe: TestProbe[IntermediateSmrResult] =
    testKit.createTestProbe[IntermediateSmrResult]

  val smrDataUploadBuilder: ActorRef[IntermediateSmrResult] =
    testKit.spawn(SmrDataUploadBuilder(interProbe.ref))

  val smrParams: SmrParams = SmrParams(
    service = "ticktok",
    post = "123",
    user = "abc",
    timestamp = DateTime.fromIsoDateTimeString("2022-06-26T02:00:00").get
  )

  "data upload builder" should {

    "create json data from user params" in {
      val expected = "{\"post\":\"123\",\"service\":\"ticktok\",\"timestamp\":\"2022-06-26T02:00:00\",\"user\":\"abc\"}"
      smrDataUploadBuilder ! ValidSmrParams(smrParams, replyProbe.ref)
      val msg = interProbe.expectMessageType[SmrUpload]
      msg.data should be(expected)
    }

    "create file key name" in {
      val clicks = "1656208800000"
      smrDataUploadBuilder ! ValidSmrParams(smrParams, replyProbe.ref)
      val msg = interProbe.expectMessageType[SmrUpload]
      msg.key should startWith(clicks)
      msg.key should endWith(".json")
    }

    "pass on replyTo" in {
      smrDataUploadBuilder ! ValidSmrParams(smrParams, replyProbe.ref)
      val msg = interProbe.expectMessageType[SmrUpload]
      msg.replyTo should be(replyProbe.ref)
    }
  }
}