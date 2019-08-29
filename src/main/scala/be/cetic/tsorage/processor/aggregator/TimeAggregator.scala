package be.cetic.tsorage.processor.aggregator

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoUnit, TemporalUnit}
import java.util.Date

import be.cetic.tsorage.processor.DAO
import be.cetic.tsorage.processor.database.Cassandra
import com.datastax.driver.core.querybuilder.QueryBuilder._
import be.cetic.tsorage.processor.sharder.Sharder
import com.datastax.driver.core.{ConsistencyLevel, Row, Session, SimpleStatement}
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.typesafe.scalalogging.LazyLogging

import collection.JavaConverters._

abstract class TimeAggregator extends LazyLogging
{
   private val raw_aggregators: Map[String, Iterable[(Date, Float)] => Float] = Map(
      "sum" ->   {values => values.map(_._2).sum},
      "count" -> {values => values.size},
      "max" ->   {values => values.map(_._2).max},
      "min" ->   {values => values.map(_._2).min}
   )

   private val raw_temp_aggregators: Map[String, Iterable[(Date, Float)] => (Date, Float)] = Map(
      "first" -> {values => values.minBy(_._1)},
      "last" ->  {values => values.maxBy(_._1)}
   )

   private val agg_aggregators: Map[String, Iterable[Float] => Float] = Map(
      "sum" ->   {values => values.sum},
      "count" -> {values => values.sum},
      "max" ->   {values => values.max},
      "min" ->   {values => values.min}
   )

   private val agg_temp_aggregators: Map[String, Iterable[(Date, Float)] => (Date, Float)] = Map(
      "first" -> {values => values.minBy(_._1)},
      "last" ->  {values => values.maxBy(_._1)}
   )

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

   def previousName: String

   def updateShunkFromRaw(
                            metric: String,
                            tagset: Map[String, String],
                            shunk: LocalDateTime
                         ): Unit =
   {
      val (shunkStart: LocalDateTime, shunkEnd: LocalDateTime) = range(shunk)
      val shards = Cassandra.sharder.shards(shunkStart, shunkEnd)

      val queries = shards.map(shard => DAO.getRawShunkValues(metric, shard, shunkStart, shunkEnd, tagset))
      logger.info(s"QUERIES: ${queries.mkString(" ; ")}")
      val statements = queries.map(q => new SimpleStatement(q).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM))

      val results = statements.par
         .map(statement => Cassandra.session.execute(statement).all().asScala)
         .reduce((l1, l2) => l1 ++ l2)
         .map(row => (row.getTimestamp("datetime"), row.getFloat("value"))).toArray

      val shunkShard = Cassandra.sharder.shard(shunk)

      // calculate and submit aggregates
      raw_aggregators.foreach(agg => Cassandra.submitValue(
         metric,
         shunkShard,
         name,
         agg._1,
         tagset,
         shunk,
         agg._2(results)
      ))

      raw_temp_aggregators.foreach(agg => {
         val transformedResult = agg._2(results)

         Cassandra.submitTemporalValue(
            metric,
            shunkShard,
            name,
            agg._1,
            tagset,
            shunk,
            transformedResult._1,
            transformedResult._2
         )
      })
   }

   def updateShunkFromAgg(
                            metric: String,
                            tagset: Map[String, String],
                            shunk: LocalDateTime
                         )
   : Unit =
   {
      val (shunkStart: LocalDateTime, shunkEnd: LocalDateTime) = range(shunk)
      val shards = Cassandra.sharder.shards(shunkStart, shunkEnd)

      agg_aggregators.par.foreach(agg => {
         val queries = shards.map(shard => DAO.getAggShunkValues(metric, shard, previousName, agg._1, shunkStart, shunkEnd, tagset))
         val statements = queries.map(q => new SimpleStatement(q).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM))

         val results = statements.par
            .map(statement => Cassandra.session.execute(statement).all().asScala)
            .reduce((l1, l2) => l1 ++ l2)
            .map(row => row.getFloat("value")).toArray

         Cassandra.submitValue(metric, Cassandra.sharder.shard(shunk), name, agg._1, tagset, shunk, agg._2(results))
      })

      agg_temp_aggregators.par.foreach(agg => {
         val queries = shards.map(shard => DAO.getAggTempShunkValues(metric, shard, previousName, agg._1, shunkStart, shunkEnd, tagset))
         val statements = queries.map(q => new SimpleStatement(q).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM))

         val results = statements.par
            .map(statement => Cassandra.session.execute(statement).all().asScala)
            .reduce((l1, l2) => l1 ++ l2)
            .map(row => (row.getTimestamp("observation_datetime"), row.getFloat("value"))).toArray

         val transformedResult = agg._2(results)

         Cassandra.submitTemporalValue(
            metric,
            Cassandra.sharder.shard(shunk),
            name,
            agg._1,
            tagset,
            shunk,
            transformedResult._1,
            transformedResult._2)
      })
   }

   def updateShunk(metric: String, tagset: Map[String, String], shunk: LocalDateTime): (String, Map[String, String], LocalDateTime) =
   {
      logger.info(s"${name} update shunk ${metric}, ${tagset}, ${shunk}")

      if(previousName == "raw") updateShunkFromRaw(metric, tagset, shunk)
      else updateShunkFromAgg(metric, tagset, shunk)

      (metric, tagset, shunk)
   }
}

abstract class SimpleTimeAggregator(val unit: TemporalUnit, val name: String, val previousName: String) extends TimeAggregator
{
   def isBorder(dt: LocalDateTime): Boolean

   def shunk(dt: LocalDateTime): LocalDateTime = if(isBorder(dt)) dt
                                                 else dt.truncatedTo(unit).plus(1, unit)

   def range(shunk: LocalDateTime): (LocalDateTime, LocalDateTime) = (shunk.minus(1, unit) , shunk)
}

/**
  * Aggretates datetimes to the next minute.
  */
object MinuteAggregator extends SimpleTimeAggregator(ChronoUnit.MINUTES, "1m", "raw")
{
   def isBorder(dt: LocalDateTime) = (dt.getSecond == 0) && (dt.getNano == 0)
}

/**
  * Aggretates datetimes to the next hour.
  */
object HourAggregator extends SimpleTimeAggregator(ChronoUnit.HOURS, "1h", "1m")
{
   def isBorder(dt: LocalDateTime) = (dt.getMinute == 0) && (dt.getSecond == 0) && (dt.getNano == 0)
}

/**
  * Aggretates datetimes to the next day.
  */
object DayAggregator extends SimpleTimeAggregator(ChronoUnit.DAYS, "1d", "1h")
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
   override def previousName = "1d"
}

