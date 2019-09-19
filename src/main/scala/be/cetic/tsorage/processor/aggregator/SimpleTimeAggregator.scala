package be.cetic.tsorage.processor.aggregator

import java.time.LocalDateTime
import java.time.temporal.TemporalUnit

/**
  * Created by Mathieu Goeminne.
  */
abstract class SimpleTimeAggregator(val unit: TemporalUnit, val name: String, val previousName: String) extends TimeAggregator
{
   def isBorder(dt: LocalDateTime): Boolean

   def shunk(dt: LocalDateTime): LocalDateTime = if(isBorder(dt)) dt
                                                 else dt.truncatedTo(unit).plus(1, unit)

   def range(shunk: LocalDateTime): (LocalDateTime, LocalDateTime) = (shunk.minus(1, unit) , shunk)
}
