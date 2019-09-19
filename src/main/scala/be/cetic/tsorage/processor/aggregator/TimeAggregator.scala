package be.cetic.tsorage.processor.aggregator

import java.time.LocalDateTime
import java.time.temporal.{ChronoUnit, TemporalUnit}
import java.util.Date

import be.cetic.tsorage.processor.{DAO, ObservationUpdate}
import be.cetic.tsorage.processor.database.Cassandra
import com.datastax.driver.core.{ConsistencyLevel, Row, Session, SimpleStatement}
import com.typesafe.scalalogging.LazyLogging

import collection.JavaConverters._

abstract class TimeAggregator extends LazyLogging
{
   private val raw_aggregators: Map[String, Iterable[(Date, Float)] => Float] = Map(
      "sum" ->   {values => values.map(_._2).sum},
      "s_sum" -> {values => values.map(x=> x._2*x._2).sum},
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
      "s_sum" -> {values => values.map(x=> x*x).sum},
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

   def shunk[T](update: ObservationUpdate[T]): ObservationUpdate[T] = ObservationUpdate(
      update.metric,
      update.tagset,
      shunk(update.datetime),
      update.interval,
      update.values
   )

   /**
     * Provides the datetime range corresponding to a particular shunk. The begining of the range is exclusive, while the end is inclusive
     * @param shunk  A shunk datetime
     * @return a tuple (a,b) such as shunk exactly covers ]a, b]
     */
   def range(shunk: LocalDateTime): (LocalDateTime, LocalDateTime)

   def name: String

   def previousName: String

   /**
     * Update the shunk corresponding to a given observation update.
     * @param update The observation update, corresponding to a change to pass on this aggregator.
     * @return The observation update corresponding to the updated shunk , The list of updated
     */
   def updateShunkFromRaw(update: ObservationUpdate[Float]): ObservationUpdate[Float] =
   {
      val (shunkStart: LocalDateTime, shunkEnd: LocalDateTime) = range(update.datetime)
      val shards = Cassandra.sharder.shards(shunkStart, shunkEnd)

      val queries = shards.map(shard => DAO.getRawShunkValues(update.metric, shard, shunkStart, shunkEnd, update.tagset))
      logger.info(s"QUERIES: ${queries.mkString(" ; ")}")
      val statements = queries.map(q => new SimpleStatement(q).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM))

      val results = statements.par
         .map(statement => Cassandra.session.execute(statement).all().asScala)
         .reduce((l1, l2) => l1 ++ l2)
         .map(row => (row.getTimestamp("datetime_"), row.getFloat("value_"))).toArray

      val shunkShard = Cassandra.sharder.shard(update.datetime)

      // calculate and submit aggregates
      val values = raw_aggregators.map(agg => agg._1 -> agg._2(results))
      values.foreach(v => Cassandra.submitValue(
         update.metric,
         shunkShard,
         name,
         v._1,
         update.tagset,
         update.datetime,
         v._2
      ))

      val temp_values = raw_temp_aggregators.map(agg => agg._1 -> agg._2(results))
      temp_values.foreach(v => Cassandra.submitTemporalValue(
         update.metric,
         shunkShard,
         name,
         v._1,
         update.tagset,
         update.datetime,
         v._2._1,
         v._2._2
      ))

      val all_values = values ++ temp_values.mapValues(v => v._2)

      ObservationUpdate(update.metric, update.tagset, update.datetime, name, all_values)
   }

   def updateShunkFromAgg(update: ObservationUpdate[Float]): ObservationUpdate[Float] =
   {
      val (shunkStart: LocalDateTime, shunkEnd: LocalDateTime) = range(update.datetime)
      val shards = Cassandra.sharder.shards(shunkStart, shunkEnd)

      val values = agg_aggregators.map(agg => {
         val queries = shards.map(shard => DAO.getAggShunkValues(update.metric, shard, previousName, agg._1, shunkStart, shunkEnd, update.tagset))
         val statements = queries.map(q => new SimpleStatement(q).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM))

         val results = statements.par
            .map(statement => Cassandra.session.execute(statement).all().asScala)
            .reduce((l1, l2) => l1 ++ l2)
            .map(row => row.getFloat("value_")).toArray

         agg._1 -> agg._2(results)
      })

      values.foreach(v => Cassandra.submitValue(update.metric, Cassandra.sharder.shard(update.datetime), name, v._1, update.tagset, update.datetime, v._2))

      val temp_values = agg_temp_aggregators.map(agg => {
         val queries = shards.map(shard => DAO.getAggTempShunkValues(update.metric, shard, previousName, agg._1, shunkStart, shunkEnd, update.tagset))
         val statements = queries.map(q => new SimpleStatement(q).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM))

         val results = statements.par
            .map(statement => Cassandra.session.execute(statement).all().asScala)
            .reduce((l1, l2) => l1 ++ l2)
            .map(row => (row.getTimestamp("observation_datetime_"), row.getFloat("value_"))).toArray

         agg._1 -> agg._2(results)
      })

      temp_values.foreach(v => Cassandra.submitTemporalValue(
         update.metric,
         Cassandra.sharder.shard(update.datetime),
         name,
         v._1,
         update.tagset,
         update.datetime,
         v._2._1,
         v._2._2
      ))

      val all_values = values ++ temp_values.mapValues(v => v._2)

      ObservationUpdate(update.metric, update.tagset, update.datetime, name, all_values)
   }

   /**
     *
     * @param update The update to perform
     * @return The observation update that has been performed
     */
   def updateShunk(update: ObservationUpdate[Float]): ObservationUpdate[Float] =
   {
      logger.info(s"${name} update shunk ${update}")

      if(previousName == "raw") updateShunkFromRaw(update)
      else updateShunkFromAgg(update)
   }

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













