package be.cetic.tsorage.processor.update

import java.time.LocalDateTime

import be.cetic.tsorage.common.TimeSeries
import be.cetic.tsorage.common.sharder.Sharder
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import spray.json.JsValue

/**
  * A representation of a change in a raw observation.
  * That corresponds to a measure to be inserted or updated in the database.
  *
  * @param ts    The updated time series.
  * @param datetime  The instant associated with the observation.
  * @param `type`    A representation of the type of value observed.
  * @param value     The observed value.
  */
class RawUpdate(
   ts: TimeSeries,
   datetime: LocalDateTime,
   `type`: String,
   value: JsValue
) extends Update(ts, datetime, `type`, value)
{
   def asTimeAggregatorUpdate(aggregator: TimeAggregator) = TimeAggregatorRawUpdate(
      ts,
      aggregator.shunk(datetime),
      `type`
   )
}
