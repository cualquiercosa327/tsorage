package be.cetic.tsorage.processor.aggregator.raw.tdouble

import java.time.LocalDateTime

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.aggregator.raw.SimpleRawDerivator
import be.cetic.tsorage.processor.datatype.DoubleSupport
import be.cetic.tsorage.processor.update.TimeAggregatorRawUpdate
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
object TDoubleSSum extends SimpleRawDerivator
{
   override def matches(ru: TimeAggregatorRawUpdate): Boolean = ru.`type` == DoubleSupport.`type`

   /**
    * Performs the aggregation of an history, for providing aggregated updates.
    *
    * For a given raw update, the matches method determines the output of this method:
    *
    * - If matches returns true, then some aggregated updates should actually be produced,
    * and the retrieved list should not be empty.
    * - Otherwise, no aggregated updates are expected, and an empty list is retrieved.
    *
    * @param ru      The involved raw update.
    * @param history The historical aggregated values corresponding to the triggering aggregated update.
    * @return New aggregated values
    */
   override def aggregate(ru: TimeAggregatorRawUpdate, history: List[(LocalDateTime, JsValue)]): List[AggUpdate] =
   {
      val ret = history.map(h => DoubleSupport.fromJson(h._2))
            .map(v => v*v)
            .sum

      List(
         AggUpdate(ru.ts, ru.ta.name, ru.shunk, DoubleSupport.`type`, DoubleSupport.asJson(ret), "s_sum")
      )
   }
}
