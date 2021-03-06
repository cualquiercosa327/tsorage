package be.cetic.tsorage.processor.aggregator.data.tdouble

import java.util.Date

import be.cetic.tsorage.processor.aggregator.data.DataAggregation
import be.cetic.tsorage.processor.datatype.{DataTypeSupport, DataValue, DoubleSupport}

/**
  * Aggregation by taking the minimum of the values.
  */
object MinimumAggregation extends DataAggregation[Double, Double]
{
   override def name: String = "min"

   override def rawAggregation(values: Iterable[(Date, Double)]): DataValue[Double] = DataValue(values.map(_._2).min, DoubleSupport)
   override def aggAggregation(values: Iterable[(Date, Double)]): DataValue[Double] = DataValue(values.map(_._2).min, DoubleSupport)

   override def rawSupport: DataTypeSupport[Double] = DoubleSupport
   override def aggSupport: DataTypeSupport[Double] = DoubleSupport
}
