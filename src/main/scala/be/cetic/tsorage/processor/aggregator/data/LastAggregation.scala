package be.cetic.tsorage.processor.aggregator.data

import java.util.Date

import be.cetic.tsorage.processor.aggregator.data.tdatex.DateXSupport
import be.cetic.tsorage.processor.datatype.{DataTypeSupport, DataValue}

/**
  * Aggregates values by taking the last of them (ie, the value with the biggest timestamp).
  */
case class LastAggregation[T](
                                rawSupport: DataTypeSupport[T],
                                aggSupport: DataTypeSupport[(Date, T)]
                             ) extends DataAggregation[T, (Date, T)]
{
   override def name: String = "last"

   override def rawAggregation(rawValues: Iterable[(Date, T)]): DataValue[(Date, T)] = DataValue(rawValues.maxBy(_._1), DateXSupport[T]())

   override def aggAggregation(aggValues: Iterable[(Date, (Date, T))]): DataValue[(Date, T)] = DataValue(aggValues.map(_._2).maxBy(_._1), DateXSupport[T]())
}