package be.cetic.tsorage.processor

import java.time.LocalDateTime

import be.cetic.tsorage.processor.datatype.{DoubleDataType, DataTypeSupport}
import spray.json.{DeserializationException, JsArray, JsNumber, JsObject, JsString, JsValue, RootJsonFormat, DefaultJsonProtocol}
import spray.json._

/**
  * A package message containing observations.
  */
case class Message[T](
                        metric: String,
                        tagset: Map[String, String],
                        values: List[(LocalDateTime, T)],
                        support: DataTypeSupport[T]
                     ) extends Serializable
{

}

object Message extends MessageJsonSupport
{
   val dataTypeSupports: List[DataTypeSupport[_]] = List(
      DoubleDataType
   )

   implicit def messageFormat[T] = new RootJsonFormat[Message[T]] {
      def write(msg: Message[T]) = new JsObject(Map(
         "metric" -> msg.metric.toJson,
         "tagset" -> msg.tagset.toJson,
         "type" -> msg.support.`type`.toJson,
         "values" -> msg.values.map(value => (value._1, msg.support.asJson(value._2))).toJson
      ))

      def read(value: JsValue): Message[T] = value match {
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

            val dataType: DataTypeSupport[T] = dataTypeSupports.find(t => t.`type` == `type`) match {
               case Some(sdt: DataTypeSupport[T]) => sdt
               case _ => throw DeserializationException(s"${`type`} is not a supported data type.")
            }

            val values = (fields("values") match {
               case JsArray(observations) => {
                  observations.map(obs => obs match {
                     case JsArray(element) => ( element(0).convertTo[LocalDateTime] , dataType.fromJson(element(1)))
                     case _ => throw DeserializationException(s"Invalid values.")
                  })
               }

               case _ => throw DeserializationException("Array expected values.")
            }).toList

            Message(metric, tagset, values, dataType)
         }

         case _ => throw DeserializationException(s"Message expected; got ${value.compactPrint} instead.")
      }
   }
}