package be.cetic.tsorage.processor.aggregator.time

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
  * Aggregates datetimes to the next minute.
  */
class MinuteAggregator(previousName: String) extends SimpleTimeAggregator(ChronoUnit.MINUTES, "1m", previousName)
{
   def isBorder(dt: LocalDateTime) = (dt.getSecond == 0) && (dt.getNano == 0)
}
