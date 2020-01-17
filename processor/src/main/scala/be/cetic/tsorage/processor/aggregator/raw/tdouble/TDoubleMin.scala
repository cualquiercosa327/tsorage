package be.cetic.tsorage.processor.aggregator.raw.tdouble

import java.time.LocalDateTime

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.aggregator.raw.SimpleRawAggregator
import be.cetic.tsorage.processor.datatype.DoubleSupport
import be.cetic.tsorage.processor.update.TimeAggregatorRawUpdate
import spray.json.JsValue

/**
 * A derivator taking the maximum values.
 */
object TDoubleMin extends SimpleRawAggregator
{
   override def matches(ru: TimeAggregatorRawUpdate): Boolean = ru.`type` == DoubleSupport.`type`

   override def aggregate(ru: TimeAggregatorRawUpdate, history: List[(LocalDateTime, JsValue)]): List[AggUpdate] = {
      val min = history.map(datapoint => DoubleSupport.fromJson(datapoint._2)).min

      List(
         AggUpdate(ru.ts, ru.ta.name, ru.shunk, DoubleSupport.`type`, DoubleSupport.asJson(min), "min")
      )
   }
}
