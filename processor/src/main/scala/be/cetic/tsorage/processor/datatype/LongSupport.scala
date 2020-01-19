package be.cetic.tsorage.processor.datatype
import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.aggregator.data.{CountAggregation, DataAggregation}
import be.cetic.tsorage.processor.datatype.DoubleSupport.{aggUDTType, rawUDTType}
import com.datastax.driver.core.UDTValue
import spray.json.{JsNumber, JsValue}

/**
  * A support object for the long data type.
  */
object LongSupport extends DataTypeSupport[Long]
{
   override val colname = "value_tlong"
   override val `type` = "tlong"

   override def asJson(value: Long): JsValue = JsNumber(value)
   override def fromJson(value: JsValue): Long = value match {
      case JsNumber(x) => x.toLong
      case _ => throw new IllegalArgumentException(s"Expected long; got ${value}")
   }

   override def fromUDTValue(value: UDTValue): Long = value.getLong("value")

   override def asRawUdtValue(value: Long): UDTValue = rawUDTType
      .newValue()
      .setLong("value", value)

   override def asAggUdtValue(value: Long): UDTValue = aggUDTType
      .newValue()
      .setLong("value", value)
}
