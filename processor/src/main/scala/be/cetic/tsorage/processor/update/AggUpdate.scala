package be.cetic.tsorage.processor.update

import java.time.LocalDateTime

import spray.json.JsValue

/**
  * A representation of an aggregated event.
  *
  * @param metric    A identifier for the object being observed. Typically, that correspond to the ID of a sensor.
  * @param tagset    A set of properties associated with this observation.
  * @param datetime  The instant associated with the observation.
  * @param `type`    A representation of the type of value observed.
  * @param value     The observed value.
  * @param interval  The time interval involved in this aggregation.
  *                  For instance, "1m" or "1h" for one minute or one hour, respectively.
  * @param aggregation A representation of the type of aggregation performed on lower-level observations.
  *                    For instance, "sum" or "count" for the sum or the counting of the observations, respectively.
  */
class AggUpdate(
   metric: String,
   tagset: Map[String, String],
   val interval: String,
   datetime: LocalDateTime,
   `type`: String,
   value: JsValue,
   val aggregation: String
) extends Update(metric, tagset, datetime, `type`, value)
