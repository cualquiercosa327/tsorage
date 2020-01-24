package be.cetic.tsorage.processor.aggregator.followup

import java.time.{LocalDateTime, ZoneOffset}

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.datatype.{DataTypeSupport, DatedTypeSupport}
import com.typesafe.scalalogging.LazyLogging
import spray.json.JsValue

object LastAggDerivator extends SimpleFollowUpDerivator with LazyLogging
{
   override def matches(au: AggUpdate): Boolean = au.`type`.startsWith("date_") && au.aggregation == "last"

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
      val support = DatedTypeSupport.inferSupport(au.`type`)
      val last = history.maxBy(h => support.fromJson(h._2).datetime.toInstant(ZoneOffset.UTC).toEpochMilli)._2

      List(
         AggUpdate(au.ts, ta.name, ta.shunk(au.datetime), support.`type`, last, "last")
      )
   }
}

