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

/**
  * A sharder based on the year-month part of the timestamp.
  */
object MonthSharder extends Sharder
{
   val shardFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

   def shard(dt: LocalDateTime) = dt.format(shardFormatter)
}

/**
  * A sharder based on the year-month-day part of the timestamp.
  */
object DaySharder extends Sharder
{
   val shardFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

   def shard(dt: LocalDateTime) = dt.format(shardFormatter)
}
