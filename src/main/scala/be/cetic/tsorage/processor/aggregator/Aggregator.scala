package be.cetic.tsorage.processor.aggregator

import java.time.LocalDateTime
import java.util.Date

import be.cetic.tsorage.processor.{DAO, ObservationUpdate}
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.datatype.SupportedDataType
import com.datastax.driver.core.{ConsistencyLevel, SimpleStatement}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._


/**
  * An entity aggregating values, and updating derivated aggregated values.
  */
case class Aggregator[T](timeAggregator: TimeAggregator, support: SupportedDataType[T]) extends LazyLogging
{
   /**
     * Update the shunk corresponding to a given observation update.
     * @param update The observation update, corresponding to a change to pass on this aggregator.
     * @return The observation update corresponding to the updated shunk , The list of updated
     */
   def updateShunkFromRaw(update: ObservationUpdate[T]): ObservationUpdate[T] =
   {
      val (shunkStart: LocalDateTime, shunkEnd: LocalDateTime) = timeAggregator.range(update.datetime)
      val shards = Cassandra.sharder.shards(shunkStart, shunkEnd)

      val queries = shards.map(shard => DAO.getRawShunkValues(update.metric, shard, shunkStart, shunkEnd, update.tagset))
      logger.info(s"QUERIES: ${queries.mkString(" ; ")}")
      val statements = queries.map(q => new SimpleStatement(q).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM))

      val results = statements.par
         .map(statement => Cassandra.session.execute(statement).all().asScala)
         .reduce((l1, l2) => l1 ++ l2)
         .map(row => (row.getTimestamp("datetime_"), row.get(support.colname, support.codec))).toArray

      val shunkShard = Cassandra.sharder.shard(update.datetime)

      // calculate and submit aggregates
      val values: Map[String, T] = support.aggregator.rawAggregators.map(agg => agg._1 -> agg._2(results))

      values.foreach(v => Cassandra.submitValue(
         update.metric,
         shunkShard,
         timeAggregator.name,
         v._1,
         update.tagset,
         update.datetime,
         support.colname,
         v._2
      ))

      val temp_values: Map[String, (Date, T)] = support.aggregator.rawTempAggregators.map(agg => agg._1 -> agg._2(results))
      temp_values.foreach(v => Cassandra.submitTemporalValue(
         update.metric,
         shunkShard,
         timeAggregator.name,
         v._1,
         update.tagset,
         update.datetime,
         v._2._1,
         support.colname,
         v._2._2
      ))

      val all_values = values ++ temp_values.mapValues(v => v._2)

      ObservationUpdate(update.metric, update.tagset, update.datetime, timeAggregator.name, all_values)
   }

   def updateShunkFromAgg(update: ObservationUpdate[T]): ObservationUpdate[T] =
   {
      val (shunkStart: LocalDateTime, shunkEnd: LocalDateTime) = timeAggregator.range(update.datetime)
      val shards = Cassandra.sharder.shards(shunkStart, shunkEnd)

      val values = support.aggregator.aggAggregators.map(agg => {
         val queries = shards.map(shard => DAO.getAggShunkValues(update.metric, shard, timeAggregator.previousName, agg._1, shunkStart, shunkEnd, update.tagset))
         val statements = queries.map(q => new SimpleStatement(q).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM))

         val results: Array[(Date, T)] = statements.par
            .map(statement => Cassandra.session.execute(statement).all().asScala)
            .reduce((l1, l2) => l1 ++ l2)
            .map(row => (row.getTimestamp("datetime_"), row.get(support.colname, support.codec))).toArray

         agg._1 -> agg._2(results)
      })

      values.foreach(v =>
         Cassandra.submitValue(
            update.metric,
            Cassandra.sharder.shard(update.datetime),
            timeAggregator.name,
            v._1,
            update.tagset,
            update.datetime,
            support.colname,
            v._2
         )
      )

      val temp_values: Map[String, (Date, T)] = support.aggregator.aggTempAggregators.map(agg => {
         val queries = shards.map(shard => DAO.getAggTempShunkValues(update.metric, shard, timeAggregator.previousName, agg._1, shunkStart, shunkEnd, update.tagset))
         val statements = queries.map(q => new SimpleStatement(q).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM))

         val results: Array[(Date, T)] = statements.par
            .map(statement => Cassandra.session.execute(statement).all().asScala)
            .reduce((l1, l2) => l1 ++ l2)
            .map(row => (row.getTimestamp("observation_datetime_"), row.get(support.colname, support.codec))).toArray

         agg._1 -> agg._2(results)
      })

      temp_values.foreach(v =>
         Cassandra.submitTemporalValue(
            update.metric,
            Cassandra.sharder.shard(update.datetime),
            timeAggregator.name,
            v._1,
            update.tagset,
            update.datetime,
            v._2._1,
            support.colname,
            v._2._2
         )
      )

      val all_values = values ++ temp_values.mapValues(v => v._2)

      ObservationUpdate(update.metric, update.tagset, update.datetime, timeAggregator.name, all_values)
   }

   /**
     *
     * @param update The update to perform
     * @return The observation update that has been performed
     */
   def updateShunk(update: ObservationUpdate[T]): ObservationUpdate[T] =
   {
      logger.info(s"${timeAggregator.name} updates shunk ${update}")

      if(timeAggregator.previousName == "raw") updateShunkFromRaw(update)
      else updateShunkFromAgg(update)
   }
}
