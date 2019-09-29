package be.cetic.tsorage.processor.aggregator.data.tdatex

import java.util.Date

import be.cetic.tsorage.processor.AggUpdate
import be.cetic.tsorage.processor.aggregator.data.DataAggregation
import be.cetic.tsorage.processor.datatype.DataTypeSupport
import com.datastax.driver.core.{DataType, TypeCodec}
import spray.json.JsValue

/**
  * Created by Mathieu Goeminne.
  */
case class DateXSupport[T]() extends DataTypeSupport[(Date, T)]
{
   override def colname: String = ???

   override def codec: TypeCodec[(Date, T)] = ???

   override def `type`: String = ???

   override def asJson(value: (Date, T)): JsValue = ???

   override def fromJson(value: JsValue): (Date, T) = ???

   override def rawAggregations: List[DataAggregation[(Date, T), _]] = ???

   /**
     * Finds the aggregation corresponding to a particular aggregated update.
     *
     * @param update The update from which an aggregation must be found.
     * @return The aggregation associated with the update.
     */
   override def findAggregation(update: AggUpdate): DataAggregation[_, (Date, T)] = ???
}
