package be.cetic.tsorage.processor.aggregator.data

import com.datastax.driver.core.TypeCodec
import spray.json.JsValue

/**
  * A value supported by the system.
  */
trait SupportedValue[T <: SupportedValue[T]]
{
   def colname: String
   def codec: TypeCodec[T]
   def `type`: String

   def asJson(): JsValue

   def aggregations: List[DataAggregation[T, _ <: SupportedValue[_]]]
}
