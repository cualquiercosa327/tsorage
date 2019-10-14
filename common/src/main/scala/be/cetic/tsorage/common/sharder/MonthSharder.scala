package be.cetic.tsorage.common.sharder

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
