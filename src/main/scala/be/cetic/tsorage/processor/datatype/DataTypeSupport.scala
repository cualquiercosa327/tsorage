package be.cetic.tsorage.processor.datatype

import java.time.LocalDateTime
import java.util.Date

import be.cetic.tsorage.processor.aggregator.data.{CountAggregation, DataAggregation}
import com.datastax.driver.core.{ConsistencyLevel, SimpleStatement, TypeCodec}
import spray.json._
import DefaultJsonProtocol._
import be.cetic.tsorage.processor.aggregator.data.tdouble.{MaximumAggregation, MinimumAggregation, SumAggregation}
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.{AggUpdate, DAO, Message, RawUpdate}
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.flow.FollowUpAggregationProcessingGraphFactory.logger
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._

/**
  * A support is an entity that helps in managing a particular type of time series value.
  * @tparam T The type of the time series
  */
abstract class DataTypeSupport[T] extends LazyLogging
{
   def colname: String
   def codec: TypeCodec[T]
   def `type`: String

   def asJson(value: T): JsValue
   def fromJson(value: JsValue): T

   def rawAggregations: List[DataAggregation[T, _]]

   def getRawValues(update: RawUpdate, shunkStart: LocalDateTime, shunkEnd: LocalDateTime): Iterable[(Date, T)] = {
      val coveringShards = Cassandra.sharder.shards(shunkStart, shunkEnd)

      val queries = coveringShards.map(shard =>
         DAO.getRawShunkValues(update.metric, shard, shunkStart, shunkEnd, update.tagset, colname))

      val statements = queries.map(q => new SimpleStatement(q).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM))

      statements.par
         .map(statement => Cassandra.session.execute(statement).all().asScala)
         .reduce((l1, l2) => l1 ++ l2)
         .map(row => (row.getTimestamp("datetime_"), row.get(colname, codec))).toIterable
   }

   /**
     * Transforms a raw update into a list of aggregated updates.
     * @param update The raw update to transform.
     * @param timeAggregator  The time aggregator involved in this transformation.
     * @return A list of aggregated updates.
     */
   def prepareAggUpdate(update: RawUpdate, timeAggregator: TimeAggregator): List[AggUpdate] =
   {
      val datetime = timeAggregator.shunk(update.datetime)
      val (shunkStart: LocalDateTime, shunkEnd: LocalDateTime) = timeAggregator.range(datetime)
      val results = getRawValues(update: RawUpdate, shunkStart: LocalDateTime, shunkEnd: LocalDateTime)

      rawAggregations.map(agg => {
         val res = agg.rawAggregation(results)
         res.asAggUpdate(update, timeAggregator, datetime, agg.name)
      })
   }

   /**
     * Finds the aggregation corresponding to a particular aggregated update.
     * @param update The update from which an aggregation must be found.
     * @return The aggregation associated with the update.
     */
   def findAggregation(update: AggUpdate): DataAggregation[_, T]

   /**
     * Transfors an aggregated update into an higher level aggregated update.
     * @param update The aggregated update to transform.
     * @param timeAggregator The time aggregator involved in this transformation.
     * @return The higher level aggregated update.
     */
   def prepareAggUpdate(update: AggUpdate, timeAggregator: TimeAggregator): AggUpdate =
   {
      val dataAggregation: DataAggregation[_,T] = findAggregation(update)
      val datetime = timeAggregator.shunk(update.datetime)

      val (shunkStart: LocalDateTime, shunkEnd: LocalDateTime) = timeAggregator.range(datetime)
      val coveringShards = Cassandra.sharder.shards(shunkStart, shunkEnd)

      val queries = coveringShards.map(shard => DAO.getAggShunkValues(
         update.metric,
         shard,
         timeAggregator.previousName,
         update.aggregation,
         shunkStart,
         shunkEnd,
         update.tagset
      ))
      logger.info(s"QUERIES: ${queries.mkString(" ; ")}")
      val statements = queries.map(q => new SimpleStatement(q).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM))

      val results = statements.par
         .map(statement => Cassandra.session.execute(statement).all().asScala)
         .reduce((l1, l2) => l1 ++ l2)
         .map(row => (row.getTimestamp("datetime_"), row.get(colname, codec))).toIterable

      val value = dataAggregation.aggAggregation(results)
      AggUpdate(update.metric, update.tagset, timeAggregator.name, datetime, update.`type`, value.asJson(), update.aggregation)
   }
}

object DataTypeSupport
{
   private def inferSupport(`type`: String) = `type` match {
      case "double" => DoubleSupport
      case "long" => LongSupport
   }

   def inferSupport(update: RawUpdate): DataTypeSupport[_] = inferSupport(update.`type`)

   def inferSupport(update: AggUpdate): DataTypeSupport[_] = inferSupport(update.`type`)
}
