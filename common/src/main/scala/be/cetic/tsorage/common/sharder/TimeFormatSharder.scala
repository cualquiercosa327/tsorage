package be.cetic.tsorage.common.sharder

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

abstract class TimeFormatSharder(val formatter: DateTimeFormatter) extends Sharder
{
  def shard(dt: LocalDateTime) = dt.format(formatter)
}
