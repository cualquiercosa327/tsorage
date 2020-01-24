package be.cetic.tsorage.processor.aggregator.raw

import java.time.{LocalDateTime, ZoneOffset}

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.datatype.{DataTypeSupport, DatedTypeSupport}
import be.cetic.tsorage.processor.update.TimeAggregatorRawUpdate
import com.typesafe.scalalogging.LazyLogging
import spray.json.{JsObject, JsString, JsValue}

/**
 * A generic approach for infering first element of an history.
 *
 */
object FirstRawDerivator extends SimpleRawDerivator with LazyLogging
{
   override def matches(ru: TimeAggregatorRawUpdate): Boolean = true

   /**
    * @param ru         The raw update to derivate.
    * @param history    The historical values to aggregate.
    * @return           The aggregated values.
    */
   def aggregate(ru: TimeAggregatorRawUpdate, history: List[(LocalDateTime, JsValue)]): List[AggUpdate] =
   {
      val rawSupport = DataTypeSupport.inferSupport(ru.`type`)
      val aggSupport = DatedTypeSupport(rawSupport)

      val first = history.minBy(_._1.toInstant(ZoneOffset.UTC).toEpochMilli)

      List(
         AggUpdate(
            ru.ts,
            ru.ta.name,
            ru.shunk,
            aggSupport.`type`,
            JsObject(Map(
               "datetime" -> JsString(DatedTypeSupport.format.format(first._1)),
               "value" -> first._2
            )),
            "first"
         )
      )
   }
}
