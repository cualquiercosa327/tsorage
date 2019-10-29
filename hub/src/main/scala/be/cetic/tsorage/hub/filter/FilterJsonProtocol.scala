package be.cetic.tsorage.hub.filter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsArray, JsString, JsValue, RootJsonFormat}

trait FilterJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport
{
   implicit object FilterFormat extends RootJsonFormat[Filter]
   {
      override def write(filter: Filter): JsValue = filter match {
         case And(a, b) => new JsArray(Vector(JsString("and"), write(a), write(b)))
         case Or(a, b) => new JsArray(Vector(JsString("or"), write(a), write(b)))
         case Not(f) => new JsArray(Vector(JsString("not"), write(f)))
         case TagFilter(name, value) => JsArray(Vector(JsString("="), JsString(name), JsString(value)))
         case TagExist(name) => JsArray(Vector(JsString("+"), JsString(name)))

      }

      override def read(json: JsValue): Filter = json match {
         case JsArray(Vector(JsString("and"), a, b)) => And(read(a), read(b))
         case JsArray(Vector(JsString("or"), a, b)) => Or(read(a), read(b))
         case JsArray(Vector(JsString("not"), f)) => Not(read(f))
         case JsArray(Vector(JsString("="), JsString(a), JsString(b))) => TagFilter(a, b)
         case JsArray(Vector(JsString("+"), JsString(name))) => TagExist(name)
      }
   }
}
