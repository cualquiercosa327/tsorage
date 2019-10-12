package be.cetic.tsorage.processor.aggregator.data.tdouble

import java.util.Date

import be.cetic.tsorage.processor.aggregator.data.DataAggregation
import be.cetic.tsorage.processor.datatype.{DataTypeSupport, DataValue, DoubleSupport}

/**
  * Aggregation by taking the maximum of the values
  */
object MaximumAggregation extends DataAggregation[Double, Double] with DoubleAggregation
{
   override def name: String = "max"

   override def rawAggregation(values: Iterable[(Date, Double)]): DataValue[Double] = DataValue(values.map(_._2).max, DoubleSupport)
   override def aggAggregation(values: Iterable[(Date, Double)]): DataValue[Double] = DataValue(values.map(_._2).max, DoubleSupport)

   override def rawSupport: DataTypeSupport[Double] = DoubleSupport
   override def aggSupport: DataTypeSupport[Double] = DoubleSupport
}
