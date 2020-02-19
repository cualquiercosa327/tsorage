package be.cetic.tsorage.common.messaging

import java.time.LocalDateTime

import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.common.messaging.message.MessagePB
import org.scalatest.{FlatSpec, Matchers}
import spray.json._

class MessageTest extends FlatSpec with Matchers with DefaultJsonProtocol with MessageJsonSupport
{
   private val content =
      """
        | {
        |   "metric": "my_sensor",
        |   "tagset": { "owner": "mg", "status": "ok" },
        |   "type": "tdouble",
        |   "values": [["2019-09-21T16:43:43", 42.167], ["2019-09-21T16:44:10", 1337.7331]]
        | }
        |""".stripMargin

   "A Double Message" should "be correctly parsed from JSON" in {
      val result = content.parseJson.convertTo[Message]

      result shouldBe Message(
         "my_sensor",
         Map[String, String]("owner" -> "mg", "status" -> "ok"),
         "tdouble",
         List[(LocalDateTime, JsValue)](
            (LocalDateTime.of(2019, 9, 21, 16, 43, 43), JsNumber(42.167)),
            (LocalDateTime.of(2019, 9, 21, 16, 44, 10), JsNumber(1337.7331))
         )
      )
   }

   it should "be correctly encoded as a Protobuf message" in {
      val result = content.parseJson.convertTo[Message]
      val encoded = Message.asPB(result)
      val bytes = encoded.toByteArray
      val recoded = MessagePB.parseFrom(bytes)
      val decoded = Message.fromPB(recoded)

      result shouldEqual decoded
   }
}