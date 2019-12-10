package be.cetic.tsorage.processor.datatype

import be.cetic.tsorage.processor.aggregator.data.{DataAggregation, LastAggregation}
import be.cetic.tsorage.processor.aggregator.data.text.{Text, TextJsonProtocol}
import be.cetic.tsorage.processor.datatype.Position2DSupport.rawUDTType
import be.cetic.tsorage.processor.update.AggUpdate
import com.datastax.driver.core.UDTValue
import spray.json.JsValue
import spray.json._

object TextSupport extends DataTypeSupport[Text] with TextJsonProtocol
{
   override def colname: String = "value_text"

   override def `type`: String = "text"

   override def asJson(value: Text): JsValue = value.toJson

   override def fromJson(value: JsValue): Text = value.convertTo[Text]

   override def asRawUdtValue(value: Text): UDTValue = rawUDTType
      .newValue()
      .setString("value", value.value)

   override def asAggUdtValue(value: Text): UDTValue = aggUDTType
      .newValue()
      .setString("value", value.value)

   override def fromUDTValue(value: UDTValue): Text = Text(value.getString("value"))

   /**
    * @return The list of all aggregations that must be applied on raw values of the supported type.
    */
   override def rawAggregations: List[DataAggregation[Text, _]] = List()

   /**
    * Finds the aggregation corresponding to a particular aggregated update.
    *
    * @param update The update from which an aggregation must be found.
    * @return The aggregation associated with the update.
    */
   override def findAggregation(update: AggUpdate): DataAggregation[_, Text] = update match {
      case _ =>
      // TODO : no relevant aggregation, change the API to support it
   }
}
