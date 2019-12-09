package be.cetic.tsorage.common.messaging


import java.time.LocalDateTime

import be.cetic.tsorage.common.json.MessageJsonSupport
import spray.json.{DeserializationException, JsArray, JsObject, JsString, JsValue, RootJsonFormat, _}

/**
 * A package message containing observations.
 */
case class Message(
                    metric: String,
                    tagset: Map[String, String],
                    `type`: String,
                    values: List[(LocalDateTime, JsValue)]
                  ) extends Serializable


object Message extends MessageJsonSupport
{
  implicit def messageFormat = new RootJsonFormat[Message] {
    def write(msg: Message) = new JsObject(Map(
      "metric" -> msg.metric.toJson,
      "tagset" -> msg.tagset.toJson,
      "type" -> msg.`type`.toJson,
      "values" -> msg.values.toJson
    ))

    def read(value: JsValue): Message = value match {
      case JsObject(fields) => {
        val metric = fields("metric") match {
          case JsString(x) => x
          case _ => throw DeserializationException("String expected as metric.")
        }

        val tagset = fields.get("tagset") match {
          case Some(JsObject(x)) => x.mapValues(value => value match { case JsString(s) => s})
          case None => Map.empty[String, String]
          case _ => throw DeserializationException("Dictionary expected as tagset.")
        }

        val `type` = fields("type") match {
          case JsString(t) => t
          case _ => throw DeserializationException("String expected as type.")
        }


        val values = (fields("values") match {
            case JsArray(observations) => {
              observations.map(obs => obs match {
                case JsArray(element) => ( element(0).convertTo[LocalDateTime] , element(1))
                case _ => throw DeserializationException(s"Invalid values.")
              })
            }

          case _ => throw DeserializationException("Array expected values.")
        }).toList

        Message(metric, tagset, `type`, values)
      }

      case _ => throw DeserializationException(s"Message expected; got ${value.compactPrint} instead.")
    }
  }
}
