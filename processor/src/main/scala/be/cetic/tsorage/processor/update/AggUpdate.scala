package be.cetic.tsorage.processor.update

import java.time.LocalDateTime

import be.cetic.tsorage.common.TimeSeries
import spray.json.JsValue

/**
  * A representation of an aggregated event.
  *
  * @param ts    The updated time series.
  * @param datetime  The instant associated with the observation.
  * @param `type`    A representation of the type of value observed.
  * @param value     The observed value.
  * @param interval  The time interval involved in this aggregation.
  *                  For instance, "1m" or "1h" for one minute or one hour, respectively.
  * @param aggregation A representation of the type of aggregation performed on lower-level observations.
  *                    For instance, "sum" or "count" for the sum or the counting of the observations, respectively.
  */
class AggUpdate(
   ts: TimeSeries,
   val interval: String,
   datetime: LocalDateTime,
   `type`: String,
   value: JsValue,
   val aggregation: String
) extends Update(ts, datetime, `type`, value)
