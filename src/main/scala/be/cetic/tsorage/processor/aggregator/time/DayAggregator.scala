package be.cetic.tsorage.processor.aggregator.time

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
  * Aggregates datetimes to the next day.
  */
class DayAggregator(previousName: String) extends SimpleTimeAggregator(ChronoUnit.DAYS, "1d", previousName)
{
   def isBorder(dt: LocalDateTime) = (dt.getHour == 0) && (dt.getMinute == 0) && (dt.getSecond == 0) && (dt.getNano == 0)
}
