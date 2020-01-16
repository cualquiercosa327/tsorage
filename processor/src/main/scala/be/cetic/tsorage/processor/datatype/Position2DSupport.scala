package be.cetic.tsorage.processor.datatype

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.aggregator.data.DataAggregation
import be.cetic.tsorage.processor.aggregator.data.position2d.{MaximumLatitudeAggregation, MaximumLongitudeAggregation, MinimumLatitudeAggregation, MinimumLongitudeAggregation, Position2D, Position2DJsonProtocol}
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
}
