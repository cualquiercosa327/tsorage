package be.cetic.tsorage.processor.aggregator.raw.tdouble

import java.time.{LocalDateTime, ZoneOffset}

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.aggregator.raw.SimpleRawAggregator
import be.cetic.tsorage.processor.datatype.{DateDoubleSupport, DoubleSupport}
import be.cetic.tsorage.processor.update.TimeAggregatorRawUpdate
import spray.json.JsValue

/**
 * Take the last value among the double data points.
 */
object LastDouble extends SimpleRawAggregator
{
   override def matches(ru: TimeAggregatorRawUpdate): Boolean = ru.`type` == DoubleSupport.`type`

   override def aggregate(ru: TimeAggregatorRawUpdate, history: List[(LocalDateTime, JsValue)]): List[AggUpdate] = {
      val last = history.maxBy(_._1.toInstant(ZoneOffset.UTC).toEpochMilli)

      List(
            AggUpdate(ru.ts, ru.ta.name, ru.shunk, DateDoubleSupport.`type`, DateDoubleSupport.asJson(last._1, DoubleSupport.fromJson(last._2)), "last")
      )
   }
}
