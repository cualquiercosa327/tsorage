package be.cetic.tsorage.processor.datatype

import be.cetic.tsorage.processor.aggregator.data.{DataAggregator, DoubleDataAggregator}
import com.datastax.driver.core.{CodecRegistry, DataType, TypeCodec}
import com.datastax.driver.core.TypeCodec.DoubleCodec
import com.datastax.oss.driver.internal.core.`type`.codec.registry.CodecRegistryConstants
import spray.json.{DeserializationException, JsNumber, JsValue}

/**
  * Support object for the double type.
  */
object DoubleDataType extends DataTypeSupport[Double]
{
   val colname = "value_double_"
   val dataAggregator: DataAggregator[Double] = DoubleDataAggregator
   val `type` = "double"
   override def asJson(value: Double) = JsNumber(value)
   override def fromJson(value: JsValue): Double = value match {
      case JsNumber(x) => x.toDouble
      case _ => throw DeserializationException(s"Double type expected; got ${value} instead.")
   }
   val codec: TypeCodec[Double] = new CodecRegistry().codecFor(DataType.cdouble())

}
