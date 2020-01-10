package be.cetic.tsorage.hub.filter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import be.cetic.tsorage.common.TimeSeries
import spray.json.{DefaultJsonProtocol, JsObject, JsString, JsValue, RootJsonFormat}

trait TimeSeriesJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport
{
   implicit object TimeSeriesFormat extends RootJsonFormat[TimeSeries]
   {
      override def write(ts: TimeSeries): JsValue = new JsObject(Map(
         "metric" -> JsString(ts.metric),
         "tagset" -> new JsObject(ts.tagset.mapValues(v => JsString(v)))
      ))

      override def read(json: JsValue): TimeSeries = {
         val fields = json.asJsObject.fields

         TimeSeries(
            fields("metric").convertTo[String],
            fields("tagset").asJsObject.fields.mapValues(v => v.convertTo[String])
         )
      }
   }
}
