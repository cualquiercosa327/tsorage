package be.cetic.tsorage.common.sharder

import java.time.LocalDateTime

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

   /**
     * @param shard  A shard
     * @return The time range corresponding to the given shard.
     */
   def range(shard: String): (LocalDateTime, LocalDateTime)

   /**
     * @param start  The beginning of a period.
     * @param end    The end of a period.
     * @return The minimal collection of shards for covering the specified period
     */
   def shards(start: LocalDateTime, end: LocalDateTime): List[String]
}

object Sharder
{
   def apply(sharder: String): Sharder = sharder match {
      case "day" => DaySharder
      case _ => MonthSharder
   }
}
