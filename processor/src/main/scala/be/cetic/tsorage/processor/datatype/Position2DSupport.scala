package be.cetic.tsorage.processor.datatype

import be.cetic.tsorage.processor.aggregator.data.DataAggregation
import be.cetic.tsorage.processor.aggregator.data.position2d.{MaximumLatitudeAggregation, MaximumLongitudeAggregation, MinimumLatitudeAggregation, MinimumLongitudeAggregation, Position2D, Position2DJsonProtocol}
import be.cetic.tsorage.processor.update.AggUpdate
import com.datastax.driver.core.UDTValue
import spray.json._

object Position2DSupport extends DataTypeSupport[Position2D] with Position2DJsonProtocol
{
   override val colname: String = "value_pos2d"

   override val `type`: String = "pos2d"

   override def asJson(value: Position2D): JsValue = value.toJson

   override def fromJson(value: JsValue): Position2D = value.convertTo[Position2D]

   /**
    * Converts a value into a Cassandra UDT Value
    *
    * @param value The value to convert
    * @return The UDTValue representing the value
    */
   override def asRawUdtValue(value: Position2D): UDTValue = rawUDTType
      .newValue()
      .setDouble("latitude", value.latitude)
      .setDouble("longitude", value.longitude)

   override def asAggUdtValue(value: Position2D): UDTValue = aggUDTType
      .newValue()
      .setDouble("latitude", value.latitude)
      .setDouble("longitude", value.longitude)

   override def fromUDTValue(value: UDTValue): Position2D = Position2D(value.getDouble("latitude"), value.getDouble("longitude"))

   /**
    * @return The list of all aggregations that must be applied on raw values of the supported type.
    */
   override def rawAggregations: List[DataAggregation[Position2D, _]] = List(
      MinimumLatitudeAggregation,
      MaximumLatitudeAggregation,
      MinimumLongitudeAggregation,
      MaximumLongitudeAggregation
   )

   /**
    * Finds the aggregation corresponding to a particular aggregated update.
    *
    * @param update The update from which an aggregation must be found.
    * @return The aggregation associated with the update.
    */
   override def findAggregation(update: AggUpdate): DataAggregation[_, Position2D] = update.aggregation match {
      case "minlat" => MinimumLatitudeAggregation
      case "maxlat" => MaximumLatitudeAggregation
      case "minlong" => MinimumLongitudeAggregation
      case "maxlong" => MaximumLongitudeAggregation
   }
}
