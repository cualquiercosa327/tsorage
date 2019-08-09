package be.cetic.tsorage.processor.sharder

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
  * A sharder is an entity responsible of partitioning time series into "buckets" or "shards",
  * each shard corresponding to a time period.
  */
trait Sharder
{
   /**
     * @param dt  A timestamp
     * @return    The shard associated with dt
     */
   def shard(dt: LocalDateTime): String
}

case class TimeFormatSharder(val formatter: DateTimeFormatter) extends Sharder
{
   def shard(dt: LocalDateTime) = dt.format(formatter)
}

/**
  * A sharder based on the year-month part of the timestamp.
  */
object MonthSharder extends TimeFormatSharder(DateTimeFormatter.ofPattern("yyyy-MM"))

/**
  * A sharder based on the year-month-day part of the timestamp.
  */
object DaySharder extends TimeFormatSharder(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
