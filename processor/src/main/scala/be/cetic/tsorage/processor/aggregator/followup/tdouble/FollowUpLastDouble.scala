package be.cetic.tsorage.processor.aggregator.followup.tdouble

import java.time.{LocalDateTime, ZoneOffset}

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.aggregator.followup.SimpleFollowUpDerivator
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.datatype.{DateDoubleSupport, DoubleSupport}
import com.typesafe.scalalogging.LazyLogging
import spray.json.JsValue

/**
 * Followup aggregation for the last double.
 */
object FollowUpLastDouble extends SimpleFollowUpDerivator with LazyLogging
{
   override def matches(au: AggUpdate): Boolean = au.aggregation == "last" && au.`type` == DateDoubleSupport.`type`

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
   override def aggregate(au: AggUpdate, ta: TimeAggregator, history: List[(LocalDateTime, JsValue)]): List[AggUpdate] = {
      val last = history.maxBy(h => DateDoubleSupport.fromJson(h._2)._1.toInstant(ZoneOffset.UTC).toEpochMilli)._2
      List(
            AggUpdate(au.ts, ta.name, ta.shunk(au.datetime), DateDoubleSupport.`type`, last, "last")
      )
   }
}
