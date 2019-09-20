package be.cetic.tsorage.processor.datatype

import be.cetic.tsorage.processor.aggregator.data.DataAggregator
import com.datastax.driver.core.TypeCodec


abstract class SupportedDataType[T]
{
   def colname: String
   def aggregator: DataAggregator[T]
   def codec: TypeCodec[T]
}

object SupportedDataType
{
   def apply(payload: Any) = payload match {
      case d: Double => DoubleDataType
   }
}
