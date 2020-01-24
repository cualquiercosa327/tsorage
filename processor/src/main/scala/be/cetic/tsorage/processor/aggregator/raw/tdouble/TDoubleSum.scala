package be.cetic.tsorage.processor.aggregator.raw.tdouble

import java.time.LocalDateTime

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.aggregator.raw.SimpleRawDerivator
import be.cetic.tsorage.processor.datatype.DoubleSupport
import be.cetic.tsorage.processor.update.TimeAggregatorRawUpdate
import spray.json.JsValue

/**
 * A derivator taking the sum of values.
 */
object TDoubleSum extends SimpleRawDerivator
{
   override def matches(ru: TimeAggregatorRawUpdate): Boolean = ru.`type` == DoubleSupport.`type`

   override def aggregate(ru: TimeAggregatorRawUpdate, history: List[(LocalDateTime, JsValue)]): List[AggUpdate] = {
      val sum = history.map(datapoint => DoubleSupport.fromJson(datapoint._2)).sum

      List(
         AggUpdate(ru.ts, ru.ta.name, ru.shunk, DoubleSupport.`type`, DoubleSupport.asJson(sum), "sum")
      )
   }
}
