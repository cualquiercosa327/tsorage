package be.cetic.tsorage.processor.datatype

import be.cetic.tsorage.processor.aggregator.data.{DataAggregator, DoubleDataAggregator}
import com.datastax.driver.core.{CodecRegistry, DataType, TypeCodec}
import com.datastax.driver.core.TypeCodec.DoubleCodec
import com.datastax.oss.driver.internal.core.`type`.codec.registry.CodecRegistryConstants

/**
  * Support object for the double type.
  */
object DoubleDataType extends SupportedDataType[Double]
{
   def colname = "value_double_"
   def aggregator: DataAggregator[Double] = DoubleDataAggregator
   val codec: TypeCodec[Double] = new CodecRegistry().codecFor(DataType.cdouble())
}
