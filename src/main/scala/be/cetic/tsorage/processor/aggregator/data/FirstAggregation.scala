package be.cetic.tsorage.processor.aggregator.data

import java.util.Date

import be.cetic.tsorage.processor.aggregator.data.tdatex.DateXSupport
import be.cetic.tsorage.processor.datatype.{DataTypeSupport, DataValue}

/**
  * Aggregates values by taking the first of them (ie, the value with the smallest timestamp).
  */
case class FirstAggregation[T](
                                rawSupport: DataTypeSupport[T],
                                aggSupport: DataTypeSupport[(Date, T)]
                             ) extends DataAggregation[T, (Date, T)]
{
   override def name: String = "first"

   override def rawAggregation(rawValues: Iterable[(Date, T)]): DataValue[(Date, T)] = DataValue(rawValues.minBy(_._1), DateXSupport[T]())

   override def aggAggregation(aggValues: Iterable[(Date, (Date, T))]): DataValue[(Date, T)] = DataValue(aggValues.map(_._2).minBy(_._1), DateXSupport[T]())
}
