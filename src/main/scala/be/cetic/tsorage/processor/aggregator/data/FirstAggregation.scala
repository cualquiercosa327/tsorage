package be.cetic.tsorage.processor.aggregator.data

import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import be.cetic.tsorage.processor.datatype.{DataTypeSupport, DataValue}

/**
  * Aggregates values by taking the first of them (ie, the value with the smallest timestamp).
  */
case class FirstAggregation[T](
                                rawSupport: DataTypeSupport[T],
                                aggSupport: DataTypeSupport[(LocalDateTime, T)]
                             ) extends DataAggregation[T, (LocalDateTime, T)]
{
   implicit val localDateOrdering: Ordering[LocalDateTime] = _ compareTo _

   override def name: String = "first"

   override def rawAggregation(rawValues: Iterable[(Date, T)]): DataValue[(LocalDateTime, T)] = {
      val firstObservation = rawValues.minBy(_._1)
      val firstDate: LocalDateTime = firstObservation._1
         .toInstant()
         .atZone(ZoneId.of("GMT"))
         .toLocalDateTime();
      val firstValue: T = firstObservation._2

      DataValue((firstDate, firstValue), aggSupport)
   }

   override def aggAggregation(aggValues: Iterable[(Date, (LocalDateTime, T))]): DataValue[(LocalDateTime, T)] =
      DataValue(aggValues.map(_._2).minBy(_._1), aggSupport)
}
