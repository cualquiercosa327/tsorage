package be.cetic.tsorage.common.json

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import be.cetic.tsorage.common.messaging.AggUpdate
import spray.json.DefaultJsonProtocol

trait AggUpdateJsonSupport extends DefaultJsonProtocol
   with SprayJsonSupport
   with MessageJsonSupport
   with TimeSeriesJsonSupport
{
   implicit val auJsonFormat = jsonFormat6(AggUpdate)
}
