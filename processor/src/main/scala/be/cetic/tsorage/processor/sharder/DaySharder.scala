package be.cetic.tsorage.processor.sharder

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
