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

   implicit val messageFormat = jsonFormat6(DoubleMessage)
   implicit val bodyFormat = jsonFormat1(DoubleBody)
   implicit val preparedMessageFormat = jsonFormat4(PreparedDoubleMessage)
   implicit val checkRunMessageFormat = jsonFormat6(CheckRunMessage)
   implicit val queryMessageFormat = jsonFormat1(AuthenticationQuery)
   implicit val userMessageFormat = jsonFormat3(User)
}
