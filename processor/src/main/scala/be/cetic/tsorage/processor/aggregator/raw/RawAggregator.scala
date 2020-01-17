package be.cetic.tsorage.processor.aggregator.raw

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.update.TimeAggregatorRawUpdate

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * A raw aggregator reacts to raw updates to produce aggregations.
 */
trait RawAggregator
{
   def aggregate(ru: TimeAggregatorRawUpdate, ta: TimeAggregator)(implicit ec: ExecutionContextExecutor): Future[List[AggUpdate]]
}
