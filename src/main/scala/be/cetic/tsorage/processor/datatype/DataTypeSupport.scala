package be.cetic.tsorage.processor.datatype

import java.time.LocalDateTime

import be.cetic.tsorage.processor.aggregator.data.DataAggregator
import com.datastax.driver.core.TypeCodec
import spray.json._
import DefaultJsonProtocol._
import be.cetic.tsorage.processor.Message

abstract class DataTypeSupport[T]
{
   def colname: String
   def dataAggregator: DataAggregator[T]
   def codec: TypeCodec[T]
   def `type`: String

   def asJson(value: T): JsValue
   def fromJson(value: JsValue): T
}
