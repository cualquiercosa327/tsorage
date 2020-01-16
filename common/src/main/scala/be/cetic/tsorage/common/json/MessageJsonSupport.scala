package be.cetic.tsorage.common.json

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import be.cetic.tsorage.common.messaging.{AuthenticationQuery, AuthenticationResponse, Message, User}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat, deserializationError}


trait MessageJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {

   implicit object LocalDateTimeJsonFormat extends RootJsonFormat[LocalDateTime] {
      def write(ldt: LocalDateTime) = JsString(ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

      def read(value: JsValue) = value match {
         case JsString(x) => LocalDateTime.parse(x, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
         case _ => deserializationError("ISO Local Date Time expected")
      }
   }

   implicit val authQueryMessageFormat = jsonFormat1(AuthenticationQuery)
   implicit val authResponseMessageFormat = jsonFormat3(AuthenticationResponse)
   implicit val userMessageFormat = jsonFormat3(User)

   implicit val messageFormat = jsonFormat4(Message)
}
