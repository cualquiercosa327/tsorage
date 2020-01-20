package be.cetic.tsorage.processor.aggregator.followup.tdouble

import java.time.LocalDateTime

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.aggregator.followup.SimpleFollowUpDerivator
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.datatype.DoubleSupport
import spray.json.JsValue

/**
 * A derivator calculating the sum of squared elements.
 *
 * It allows to compute the standard deviation or variance aggregate according to
 *
 * variance = sum(x**2)/N - mean**2
 *
 * (mean and N being straightforwardly obtained by other aggregates)
 */
object FollowUpDoubleSSum extends SimpleFollowUpDerivator
{
   override def matches(au: AggUpdate): Boolean = au.`type` == DoubleSupport.`type` && au.aggregation == "s_sum"

   /**
    * Performs the aggregation of an history, for providing extra aggregated updates.
    *
    * For a given aggregated update, the matches method determines the output of this method:
    *
    * - If matches returns true, then some aggregated updates should actually be produced,
    * and the retrieved list should not be empty.
    * - Otherwise, no aggregated updates are expected, and an empty list is retrieved.
    *
    * @param au      The aggregated update that triggers the aggregation.
    * @param ta      The time aggregator to use.
    * @param history The historical aggregated values corresponding to the triggering aggregated update.
    * @return New aggregated values
    */
   override def aggregate(au: AggUpdate, ta: TimeAggregator, history: List[(LocalDateTime, JsValue)]): List[AggUpdate] =
   {
      val sum = history.map(datapoint => DoubleSupport.fromJson(datapoint._2)).sum

      List(
         AggUpdate(au.ts, ta.name, ta.shunk(au.datetime), DoubleSupport.`type`, DoubleSupport.asJson(sum), "s_sum")
      )
   }
}
