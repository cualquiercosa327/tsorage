package be.cetic.tsorage.processor.aggregator.data.tdouble

import java.util.Date

import be.cetic.tsorage.processor.aggregator.data.DataAggregation
import be.cetic.tsorage.processor.datatype.{DataTypeSupport, DataValue, DoubleSupport}

/**
  * Aggregation by taking the sum of the values.
  */
object SumAggregation extends DataAggregation[Double, Double] with DoubleAggregation
{
   override def name: String = "sum"

   override def rawAggregation(values: Iterable[(Date, Double)]): DataValue[Double] = DataValue(values.map(_._2).sum, DoubleSupport)
   override def aggAggregation(values: Iterable[(Date, Double)]): DataValue[Double] = DataValue(values.map(_._2).sum, DoubleSupport)

   override def rawSupport: DataTypeSupport[Double] = DoubleSupport
   override def aggSupport: DataTypeSupport[Double] = DoubleSupport
}
