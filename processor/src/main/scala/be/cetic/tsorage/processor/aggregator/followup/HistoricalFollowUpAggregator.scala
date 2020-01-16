package be.cetic.tsorage.processor.aggregator.followup

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.datatype.DataTypeSupport
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * An aggregator, converting aggupdates into follow up aggupdates, by retrieving the historical
 * values associated with the received aggupdate, and processing derivators.
 */
case class HistoricalFollowUpAggregator(derivators: List[SimpleFollowUpDerivator]) extends AggAggregator with LazyLogging
{
   override def aggregate(au: AggUpdate, ta: TimeAggregator)(implicit ec: ExecutionContextExecutor): Future[List[AggUpdate]] = {

      val matchingDerivators = derivators.filter(derivator => derivator.matches(au))

      matchingDerivators match {
         case Nil => Future(Nil)
         case ms => {
            val support = DataTypeSupport.inferSupport(au.`type`)
            val history = support.getHistoryValues(au, ta)

            history.map(h => {
               ms.flatMap(derivator => derivator.aggregate(au, ta, h))
            })
         }
      }
   }
}
