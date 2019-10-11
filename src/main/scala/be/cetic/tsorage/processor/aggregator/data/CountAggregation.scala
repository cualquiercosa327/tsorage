package be.cetic.tsorage.processor.aggregator.data
import java.util.Date

import be.cetic.tsorage.processor.datatype.{DataTypeSupport, DataValue, LongSupport}



/**
  * Aggregates values by counting them.
  */
case class CountAggregation[T](rawSupport: DataTypeSupport[T]) extends DataAggregation[T, Long]
{
   override def name: String = "count"

   override def rawAggregation(rawValues: Iterable[(Date, T)]): DataValue[Long] = DataValue(rawValues.size, LongSupport)
   override def aggAggregation(aggValues: Iterable[(Date, Long)]): DataValue[Long] = DataValue(aggValues.map(_._2).sum, LongSupport)

   override def aggSupport: DataTypeSupport[Long] = LongSupport
}
