package be.cetic.tsorage.processor.aggregator.time

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
  * Aggregates datetimes to the next hour.
  */
class HourAggregator(previousName: String) extends SimpleTimeAggregator(ChronoUnit.HOURS, "1h", previousName)
{
   def isBorder(dt: LocalDateTime) = (dt.getMinute == 0) && (dt.getSecond == 0) && (dt.getNano == 0)
}
