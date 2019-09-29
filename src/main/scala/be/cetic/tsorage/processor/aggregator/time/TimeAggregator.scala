package be.cetic.tsorage.processor.aggregator.time

import java.time.LocalDateTime

import be.cetic.tsorage.processor.aggregator._
import be.cetic.tsorage.processor.aggregator.data.SupportedValue
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.{AggUpdate, DAO, RawUpdate}
import com.datastax.driver.core.{ConsistencyLevel, SimpleStatement}
import com.typesafe.scalalogging.LazyLogging

abstract class TimeAggregator() extends LazyLogging
{
    /**
     * Provides the moment to which a particular datetime will be aggregated.
     *
     * @param dt  The datetime to aggregate
     * @return    The moment at which the datetime will be aggregated
     */
   def shunk(dt: LocalDateTime): LocalDateTime

   def shunk[T <: SupportedValue[T]](update: RawUpdate): RawUpdate = RawUpdate(
      update.metric,
      update.tagset,
      shunk(update.datetime),
      update.`type`,
      update.value
   )

   def shunk[A <: SupportedValue[A]](update: AggUpdate): AggUpdate = AggUpdate(
      update.metric,
      update.tagset,
      update.interval,
      shunk(update.datetime),
      update.`type`,
      update.value,
      update.aggregation
   )

   /**
     * Provides the datetime range corresponding to a particular shunk. The begining of the range is exclusive, while the end is inclusive
     * @param shunk  A shunk datetime
     * @return a tuple (a,b) such as shunk exactly covers ]a, b]
     */
   def range(shunk: LocalDateTime): (LocalDateTime, LocalDateTime)

   def name: String

   def previousName: String



   override def toString = s"Aggregator(${name}, ${previousName})"
}

object TimeAggregator
{
   def apply(name: String, previous: String): TimeAggregator = name match {
      case "1m" => new MinuteAggregator(previous)
      case "1h" => new HourAggregator(previous)
      case "1d" => new DayAggregator(previous)
      case "1mo" => new MonthAggregator(previous)
      case _ => throw InvalidTimeAggregatorName(name)
   }
}













