package be.cetic.tsorage.processor.aggregator

import java.time.LocalDateTime
import java.time.temporal.{ChronoUnit, TemporalUnit}

import com.typesafe.scalalogging.LazyLogging


trait TimeAggregator extends LazyLogging{
   /**
     * Provides the moment to which a particular datetime will be aggregated.
     *
     * @param dt  The datetime to aggregate
     * @return    The moment at which the datetime will be aggregated
     */
   def shunk(dt: LocalDateTime): LocalDateTime

   /**
     * Provides the datetime range corresponding to a particular shunk. The begining of the range is exclusive, while the end is inclusive
     * @param shunk  A shunk datetime
     * @return a tuple (a,b) such as shunk exactly covers ]a, b]
     */
   def range(shunk: LocalDateTime): (LocalDateTime, LocalDateTime)

   def name: String

   def updateShunk(metric: String, tagset: Map[String, String], shunk: LocalDateTime): (String, Map[String, String], LocalDateTime) =
   {
      logger.info(s"${name} update shunk ${metric}, ${tagset}, ${shunk}")


      (metric, tagset, shunk)
   }
}

abstract class SimpleTimeAggregator(val unit: TemporalUnit, val name: String) extends TimeAggregator
{
   def isBorder(dt: LocalDateTime): Boolean

   def shunk(dt: LocalDateTime): LocalDateTime = if(isBorder(dt)) dt
                                                 else dt.truncatedTo(unit).plus(1, unit)

   def range(shunk: LocalDateTime): (LocalDateTime, LocalDateTime) = (shunk.minus(1, unit) , shunk)
}

/**
  * Aggretates datetimes to the next minute.
  */
object MinuteAggregator extends SimpleTimeAggregator(ChronoUnit.MINUTES, "1m")
{
   def isBorder(dt: LocalDateTime) = (dt.getSecond == 0) && (dt.getNano == 0)
}

/**
  * Aggretates datetimes to the next hour.
  */
object HourAggregator extends SimpleTimeAggregator(ChronoUnit.HOURS, "1h")
{
   def isBorder(dt: LocalDateTime) = (dt.getMinute == 0) && (dt.getSecond == 0) && (dt.getNano == 0)
}

/**
  * Aggretates datetimes to the next day.
  */
object DayAggregator extends SimpleTimeAggregator(ChronoUnit.DAYS, "1d")
{
   def isBorder(dt: LocalDateTime) = (dt.getHour == 0) && (dt.getMinute == 0) && (dt.getSecond == 0) && (dt.getNano == 0)
}

/**
  * Aggretates datetimes to the next month.
  */
object MonthAggregator extends TimeAggregator
{
   private def isBorder(dt: LocalDateTime) =
      (dt.getDayOfMonth == 1) &&
      (dt.getHour == 0) &&
      (dt.getMinute == 0) &&
      (dt.getSecond == 0) &&
      (dt.getNano == 0)

   override def shunk(dt: LocalDateTime): LocalDateTime = if(isBorder(dt)) dt
                                                          else dt.plus(1, ChronoUnit.MONTHS)
                                                                .withDayOfMonth(1)
                                                                .withHour(0)
                                                                .withMinute(0)
                                                                .withSecond(0)
                                                                .withNano(0)


   override def range(shunk: LocalDateTime): (LocalDateTime, LocalDateTime) = (shunk.minus(1, ChronoUnit.MONTHS), shunk)

   override def name = "1mo"
}

