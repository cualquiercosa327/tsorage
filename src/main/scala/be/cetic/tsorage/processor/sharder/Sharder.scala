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

abstract class TimeFormatSharder(val formatter: DateTimeFormatter) extends Sharder
{
   def shard(dt: LocalDateTime) = dt.format(formatter)
}

/**
  * A sharder based on the year-month part of the timestamp.
  */
object MonthSharder extends TimeFormatSharder(DateTimeFormatter.ofPattern("yyyy-MM"))
{
   val date = """(\d{4})-(\d{2})""".r

   override def shards(start: LocalDateTime, end: LocalDateTime): List[String] = {

      def recShards(end: LocalDateTime, current: LocalDateTime, ret: List[String]): List[String] = {
         val currentShard = shard(current)
         val currentRange = range(currentShard)

         if(currentRange._1 isAfter end) ret
         else recShards(end, current.plusMonths(1),  ret :+ (shard(current)))
      }

      recShards(end, start, Nil)
   }

   /**
     * @param shard A shard
     * @return The time range corresponding to the given shard.
     */
   override def range(shard: String): (LocalDateTime, LocalDateTime) = shard match {
      case date(year, month) => {
         val start = LocalDateTime.of(year.toInt, month.toInt, 1, 0, 0, 0, 0)
         val end = start.plusDays(31).withDayOfMonth(1).minusNanos(1)

         (start, end)
      }
   }
}

/**
  * A sharder based on the year-month-day part of the timestamp.
  */
object DaySharder extends TimeFormatSharder(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
{
   val date = """(\d{4})-(\d{2})-(\d{2})""".r

   override def shards(start: LocalDateTime, end: LocalDateTime): List[String] = {

      def recShards(end: LocalDateTime, current: LocalDateTime, ret: List[String]): List[String] = {
         val currentShard = shard(current)
         val currentRange = range(currentShard)

         if(currentRange._1 isAfter end) ret
         else recShards(end, current.plusDays(1), ret :+ shard(current))
      }

      recShards(end, start, Nil)
   }

   /**
     * @param shard A shard
     * @return The time range corresponding to the given shard.
     */
   override def range(shard: String): (LocalDateTime, LocalDateTime) = shard match {
      case date(year, month, day) => {
         val start = LocalDateTime.of(year.toInt, month.toInt, day.toInt, 0, 0, 0, 0)
         val end = start.plusDays(1).minusNanos(1)

         (start, end)
      }
   }
}
