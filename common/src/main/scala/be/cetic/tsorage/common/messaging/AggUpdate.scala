package be.cetic.tsorage.common.messaging

import java.time.LocalDateTime

import be.cetic.tsorage.common.TimeSeries
import be.cetic.tsorage.common.sharder.Sharder
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
case class AggUpdate(
   ts: TimeSeries,
   interval: String,
   datetime: LocalDateTime,
   `type`: String,
   value: JsValue,
   aggregation: String
) extends InformationVector
{
   override def metric = ts.metric
   override def tagset = ts.tagset

   override def splitByShard(sharder: Sharder) = List(ShardedInformationVector(
      ts.metric,
      sharder.shard(datetime),
      tagset
   ))

   override def splitByTagAndShard(sharder: Sharder) = tagset.map(tag =>
      ShardedTagUpdate(
         ts.metric,
         sharder.shard(datetime),
         tag._1,
         tag._2,
         tagset
      )
   )
}
