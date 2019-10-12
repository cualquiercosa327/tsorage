package be.cetic.tsorage.processor.aggregator.data.tdouble

import be.cetic.tsorage.processor.aggregator.data.{CountAggregation, DataAggregation}
import be.cetic.tsorage.processor.datatype.DoubleSupport
import be.cetic.tsorage.processor.update.AggUpdate


trait DoubleAggregation
{
   def findAggregation(update: AggUpdate): DataAggregation[Double, _] = update.aggregation match {
      case "sum" => SumAggregation
      case "max" => MaximumAggregation
      case "min" => MinimumAggregation
      case "count" => CountAggregation[Double](DoubleSupport)
   }
}