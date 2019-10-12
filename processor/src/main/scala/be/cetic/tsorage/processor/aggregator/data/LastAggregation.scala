package be.cetic.tsorage.processor.aggregator.data

import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import be.cetic.tsorage.processor.datatype.{DataTypeSupport, DataValue}

/**
  * Aggregates values by taking the last of them (ie, the value with the biggest timestamp).
  */
case class LastAggregation[T](
                                rawSupport: DataTypeSupport[T],
                                aggSupport: DataTypeSupport[(LocalDateTime, T)]
                             ) extends DataAggregation[T, (LocalDateTime, T)]
{
   implicit val localDateOrdering: Ordering[LocalDateTime] = _ compareTo _

   override def name: String = "last"

   override def rawAggregation(rawValues: Iterable[(Date, T)]): DataValue[(LocalDateTime, T)] =
   {
      val lastObservation = rawValues.maxBy(_._1)
      val lastDate: LocalDateTime = lastObservation._1
         .toInstant()
         .atZone(ZoneId.of("GMT"))
         .toLocalDateTime();
      val lastValue: T = lastObservation._2

      DataValue((lastDate, lastValue), aggSupport)
   }

   override def aggAggregation(aggValues: Iterable[(Date, (LocalDateTime, T))]): DataValue[(LocalDateTime, T)] =
      DataValue(aggValues.map(_._2).maxBy(_._1), aggSupport)
}