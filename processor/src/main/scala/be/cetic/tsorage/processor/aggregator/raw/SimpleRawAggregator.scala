package be.cetic.tsorage.processor.aggregator.raw

import java.time.LocalDateTime

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.update.TimeAggregatorRawUpdate
import spray.json.JsValue

/**
 * A simple aggregator, converting a raw history into a list of aggregated values.
 */
trait SimpleRawAggregator
{
   def aggregate(ru: TimeAggregatorRawUpdate, history: List[(LocalDateTime, JsValue)]): List[AggUpdate]
}
