package be.cetic.tsorage.processor.datatype

import java.time.LocalDateTime

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.update.TimeAggregatorRawUpdate
import spray.json.JsValue

case class DataValue[T](val value: T, support: DataTypeSupport[T])
{
   def `type`(): String = support.`type`
   def asJson(): JsValue = support.asJson(value)

   def asAggUpdate(
                     rawUpdate: TimeAggregatorRawUpdate,
                     timeAggregator: TimeAggregator,
                     datetime: LocalDateTime,
                     dataAggregation: String
                  ): AggUpdate =
      new AggUpdate(
         rawUpdate.ts,
         timeAggregator.name,
         datetime,
         support.`type`,
         asJson(),
         dataAggregation
      )
}
