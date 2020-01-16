package be.cetic.tsorage.common.json

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import be.cetic.tsorage.common.TimeSeries
import be.cetic.tsorage.common.messaging.AggUpdate
import spray.json.DefaultJsonProtocol

trait TimeSeriesJsonSupport extends DefaultJsonProtocol
   with SprayJsonSupport
{
   implicit val tsJsonFormat = jsonFormat2(TimeSeries)
}
