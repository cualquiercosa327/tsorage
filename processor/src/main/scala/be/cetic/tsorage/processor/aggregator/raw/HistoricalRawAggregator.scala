package be.cetic.tsorage.processor.aggregator.raw
import java.util.Date

import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.datatype.DataTypeSupport
import be.cetic.tsorage.processor.update.TimeAggregatorRawUpdate
import spray.json.JsValue

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * A raw aggregator based on the historical dataset associated with the raw update.
 *
 * The historical aggregator first grab the data values associated with a raw update,
 * then perform a list of data aggregators, in such a way data extraction is performed only once.
 *
 * Since aggregated values are typically based on raw data values, this is the typical
 * way to implement aggregations from raw values.
 *
 * If no aggregators are embedded in the historical aggregator, the data extraction is not performed.
 */
case class HistoricalRawAggregator(derivators: List[SimpleRawDerivator]) extends RawAggregator
{
   override def aggregate(ru: TimeAggregatorRawUpdate, ta: TimeAggregator)(implicit ec: ExecutionContextExecutor): Future[List[AggUpdate]] =
   {
      val matchingDerivators = derivators.filter(derivator => derivator.matches(ru))

      matchingDerivators match {
         case Nil => Future(Nil)
         case ms => {
            val support = DataTypeSupport.inferSupport(ru.`type`)
            val ta = ru.ta
            val (start, end) = ta.range(ru.shunk)

            val history = support.getRawValues(ru, start, end)

            history.map(h => {
               ms.flatMap(derivator => derivator.aggregate(ru, h))
            })
         }
      }
   }
}
