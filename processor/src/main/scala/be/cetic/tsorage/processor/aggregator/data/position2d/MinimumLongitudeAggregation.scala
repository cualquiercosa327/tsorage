package be.cetic.tsorage.processor.aggregator.data.position2d

import java.util.Date

import be.cetic.tsorage.processor.aggregator.data.DataAggregation
import be.cetic.tsorage.processor.datatype.{DataTypeSupport, DataValue, Position2DSupport}

/**
 * An aggregation retrieving the Position2D having the smallest longitude.
 */
object MinimumLongitudeAggregation extends DataAggregation[Position2D, Position2D]
{
   override def name: String = "minlong"

   override def rawAggregation(values: Iterable[(Date, Position2D)]): DataValue[Position2D] = DataValue(values.map(_._2).minBy(_.longitude), Position2DSupport)
   override def aggAggregation(values: Iterable[(Date, Position2D)]): DataValue[Position2D] = DataValue(values.map(_._2).minBy(_.longitude), Position2DSupport)

   override def rawSupport: DataTypeSupport[Position2D] = Position2DSupport
   override def aggSupport: DataTypeSupport[Position2D] = Position2DSupport
}