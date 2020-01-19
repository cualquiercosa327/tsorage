package be.cetic.tsorage.processor.datatype

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.aggregator.data.position2d.Position2D
import be.cetic.tsorage.processor.aggregator.data.{DataAggregation, LastAggregation}
import be.cetic.tsorage.processor.aggregator.data.text.{Text, TextJsonProtocol}
import be.cetic.tsorage.processor.datatype.Position2DSupport.{aggUDTType, rawUDTType}
import com.datastax.driver.core.UDTValue
import spray.json.JsValue
import spray.json._

object TextSupport extends DataTypeSupport[Text] with TextJsonProtocol
{
   override val colname: String = "value_ttext"

   override val `type`: String = "ttext"

   override def asJson(value: Text): JsValue = value.toJson

   override def fromJson(value: JsValue): Text = value.convertTo[Text]

   override def fromUDTValue(value: UDTValue): Text = Text(value.getString("value"))

   override def asRawUdtValue(value: Text): UDTValue = rawUDTType
      .newValue()
      .setString("value", value.value)

   override def asAggUdtValue(value: Text): UDTValue = aggUDTType
      .newValue()
      .setString("value", value.value)
}
