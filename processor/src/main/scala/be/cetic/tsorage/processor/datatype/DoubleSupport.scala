package be.cetic.tsorage.processor.datatype

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.aggregator.data.tdouble.{MaximumAggregation, MinimumAggregation, SumAggregation}
import be.cetic.tsorage.processor.aggregator.data.{CountAggregation, DataAggregation, FirstAggregation, LastAggregation}
import com.datastax.driver.core.UDTValue
import spray.json.{JsNumber, JsValue}

/**
  * Support object for double data type.
  */
object DoubleSupport extends DataTypeSupport[Double]
{
   override val colname = "value_tdouble"
   override val `type` = "tdouble"

   override def asJson(value: Double): JsValue = JsNumber(value)
   override def fromJson(value: JsValue): Double = value match {
      case JsNumber(x) => x.toDouble
      case _ => throw new IllegalArgumentException(s"Expected double; got ${value}")
   }

   override def fromUDTValue(value: UDTValue): Double = value.getDouble("value")

   override def asRawUdtValue(value: Double): UDTValue = rawUDTType
      .newValue()
      .setDouble("value", value)

   override def asAggUdtValue(value: Double): UDTValue = aggUDTType
      .newValue()
      .setDouble("value", value)
}
