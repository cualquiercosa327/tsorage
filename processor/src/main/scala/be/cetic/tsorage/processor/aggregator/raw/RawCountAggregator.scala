package be.cetic.tsorage.processor.aggregator.raw
import java.sql.Date
import java.time.LocalDateTime

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.datatype.LongSupport
import be.cetic.tsorage.processor.update.TimeAggregatorRawUpdate
import spray.json.{JsNumber, JsValue}

/**
 * An aggregator counting the number of raw observations.
 */
object RawCountAggregator extends SimpleRawDerivator
{
   override def matches(ru: TimeAggregatorRawUpdate): Boolean = true

   override def aggregate(ru: TimeAggregatorRawUpdate, history: List[(LocalDateTime, JsValue)]): List[AggUpdate] =
   {
      List(
         AggUpdate(
            ru.ts,
            ru.ta.name,
            ru.shunk,
            LongSupport.`type`,
            LongSupport.asJson(history.size),
            "count"
         )
      )
   }
}
