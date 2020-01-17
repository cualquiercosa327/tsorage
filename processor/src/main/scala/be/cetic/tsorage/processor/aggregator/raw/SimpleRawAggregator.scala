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
   def matches(ru: TimeAggregatorRawUpdate): Boolean

   /**
    * Performs the aggregation of an history, for providing aggregated updates.
    *
    * For a given raw update, the matches method determines the output of this method:
    *
    * - If matches returns true, then some aggregated updates should actually be produced,
    * and the retrieved list should not be empty.
    * - Otherwise, no aggregated updates are expected, and an empty list is retrieved.
    *
    * @param ru         The involved raw update.
    * @param history    The historical aggregated values corresponding to the triggering aggregated update.
    * @return           New aggregated values
    */
   def aggregate(ru: TimeAggregatorRawUpdate, history: List[(LocalDateTime, JsValue)]): List[AggUpdate]
}
