package be.cetic.tsorage.processor.datatype

import java.time.LocalDateTime

import be.cetic.tsorage.processor.aggregator.data.DataAggregation
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.{AggUpdate, RawUpdate}
import spray.json.JsValue

/**
  * Created by Mathieu Goeminne.
  */
case class DataValue[T](val value: T, support: DataTypeSupport[T])
{
   def `type`(): String = support.`type`
   def asJson(): JsValue = support.asJson(value)

   def asAggUpdate(
                     rawUpdate: RawUpdate,
                     timeAggregator: TimeAggregator,
                     datetime: LocalDateTime,
                     dataAggregation: String
                  ): AggUpdate =
      AggUpdate(
         rawUpdate.metric,
         rawUpdate.tagset,
         timeAggregator.name,
         datetime,
         support.`type`,
         asJson(),
         dataAggregation
      )
}
