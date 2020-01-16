package be.cetic.tsorage.processor.aggregator.data

import java.util.Date

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.datatype.{DataTypeSupport, DataValue}

/**
  * A way to aggregate data values.
  */
trait DataAggregation[T, A]
{
   def name: String
   def rawAggregation(rawValues: Iterable[(Date, T)]): DataValue[A]
   def aggAggregation(aggValues: Iterable[(Date, A)]): DataValue[A]

   def rawSupport: DataTypeSupport[T]
   def aggSupport: DataTypeSupport[A]

   def aggAggregate(update: AggUpdate, timeAggregator: TimeAggregator): AggUpdate = ???
}
