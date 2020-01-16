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
object TDoubleMax extends SimpleRawAggregator
{
   override def aggregate(ru: TimeAggregatorRawUpdate, history: List[(LocalDateTime, JsValue)]): List[AggUpdate] = {
      val max = history.map(datapoint => DoubleSupport.fromJson(datapoint._2)).max

      List(
         AggUpdate(ru.ts, ru.ta.name, ru.shunk, DoubleSupport.`type`, DoubleSupport.asJson(max), "max")
      )
   }
}
