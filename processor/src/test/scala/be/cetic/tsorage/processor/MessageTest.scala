package be.cetic.tsorage.processor

import java.time.LocalDateTime

import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.common.messaging.Message
import org.scalatest.{FlatSpec, Matchers}
import spray.json._

class MessageTest extends FlatSpec with Matchers with DefaultJsonProtocol with MessageJsonSupport
{
   "A Double Message" should "be correctly parsed from JSON" in {
      val content =
         """
           | {
           |   "metric": "my_sensor",
           |   "tagset": { "owner": "mg", "status": "ok" },
           |   "type": "double",
           |   "values": [["2019-09-21T16:43:43", 42.167], ["2019-09-21T16:44:10", 1337.7331]]
           | }
           |""".stripMargin

      val result = content.parseJson.convertTo[Message]

      result shouldBe Message(
         "my_sensor",
         Map[String, String]("owner" -> "mg", "status" -> "ok"),
         "double",
         List[(LocalDateTime, JsValue)](
            (LocalDateTime.of(2019, 9, 21, 16, 43, 43), JsNumber(42.167)),
            (LocalDateTime.of(2019, 9, 21, 16, 44, 10), JsNumber(1337.7331))
         )
      )
   }


}
