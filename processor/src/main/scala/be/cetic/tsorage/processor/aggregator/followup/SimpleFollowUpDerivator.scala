package be.cetic.tsorage.processor.aggregator.followup

import java.time.LocalDateTime

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import spray.json.JsValue

/**
 * A followup aggregator (ie, an aggregator generating aggregated updates based on an other aggregated update,
 * that aggregates historical values. This is supposed to be the most common type of aggregator.
 */
trait SimpleFollowUpDerivator
{
   def matches(au: AggUpdate): Boolean

   /**
    * Performs the aggregation of an history, for providing extra aggregated updates.
    *
    * For a given aggregated update, the matches method determines the output of this method:
    *
    * - If matches returns true, then some aggregated updates should actually be produced,
    * and the retrieved list should not be empty.
    * - Otherwise, no aggregated updates are expected, and an empty list is retrieved.
    *
    * @param au         The aggregated update that triggers the aggregation.
    * @param ta         The time aggregator to use.
    * @param history    The historical aggregated values corresponding to the triggering aggregated update.
    * @return           New aggregated values
    */
   def aggregate(au: AggUpdate, ta: TimeAggregator, history: List[(LocalDateTime, JsValue)]): List[AggUpdate]
}
