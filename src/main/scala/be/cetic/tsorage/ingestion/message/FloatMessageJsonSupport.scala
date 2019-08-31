package be.cetic.tsorage.ingestion.message

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

trait FloatMessageJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
   implicit object LocalDateTimeJsonFormat extends RootJsonFormat[LocalDateTime] {
      def write(ldt: LocalDateTime) = JsString(ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

      def read(value: JsValue) = value match {
         case JsString(x) => LocalDateTime.parse(x, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
         case _ => deserializationError("ISO Local Date Time expected")
      }
   }

   implicit val messageFormat = jsonFormat6(FloatMessage)
   implicit val bodyFormat = jsonFormat1(FloatBody)
   implicit val preparedMessageFormat = jsonFormat3(PreparedFloatMessage)
}
