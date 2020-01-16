package be.cetic.tsorage.processor.aggregator.followup

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * A followup aggregator, converting aggupdate into more aggupdates.
 */
trait AggAggregator
{
   def aggregate(au: AggUpdate, ta: TimeAggregator)(implicit ec: ExecutionContextExecutor): Future[List[AggUpdate]]
}
