package be.cetic.tsorage.processor.datatype

import java.time.LocalDateTime
import java.util.Date

import be.cetic.tsorage.processor.aggregator.data.DataAggregation
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.{AggUpdate, DAO, ProcessorConfig, RawUpdate}
import com.datastax.driver.core.{ConsistencyLevel, SimpleStatement, UDTValue, UserType}
import com.typesafe.scalalogging.LazyLogging
import spray.json._

import scala.collection.JavaConverters._

/**
  * A support is an entity that helps in managing a particular type of time series value.
  * @tparam T The type of the time series
  */
abstract class DataTypeSupport[T] extends LazyLogging
{
   def colname: String
   def `type`: String

   protected val rawUDTType : UserType = Cassandra.session
      .getCluster
      .getMetadata
      .getKeyspace(ProcessorConfig.rawKS)
      .getUserType(s"${`type`}")

   protected val aggUDTType : UserType = Cassandra.session
      .getCluster
      .getMetadata
      .getKeyspace(ProcessorConfig.rawKS)
      .getUserType(s"${`type`}")

   def asJson(value: T): JsValue
   def fromJson(value: JsValue): T

   /**
     * Converts a value into a Cassandra UDT Value
     * @param value The value to convert
     * @return The UDTValue representing the value
     */
   def asRawUdtValue(value: T): UDTValue
   def asAggUdtValue(value: T): UDTValue

   def fromUDTValue(value: UDTValue): T


   /**
     * @return The list of all aggregations that must be applied on raw values of the supported type.
     */
   def rawAggregations: List[DataAggregation[T, _]]

   def getRawValues(update: RawUpdate, shunkStart: LocalDateTime, shunkEnd: LocalDateTime): Iterable[(Date, T)] = {
      val coveringShards = Cassandra.sharder.shards(shunkStart, shunkEnd)

      val queries = coveringShards.map(shard =>
         DAO.getRawShunkValues(update.metric, shard, shunkStart, shunkEnd, update.tagset, colname))

      val statements = queries.map(q => new SimpleStatement(q).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM))

      statements.par
         .map(statement => Cassandra.session.execute(statement).all().asScala)
         .reduce((l1, l2) => l1 ++ l2)
         .map(row => (row.getTimestamp("datetime_"), fromUDTValue(row.getUDTValue(colname)))).toIterable
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
         colname,
         shunkStart,
         shunkEnd,
         update.tagset
      ))
      logger.info(s"QUERIES: ${queries.mkString(" ; ")}")
      val statements = queries.map(q => new SimpleStatement(q).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM))

      val results = statements.par
         .map(statement => Cassandra.session.execute(statement).all().asScala)
         .reduce((l1, l2) => l1 ++ l2)
         .map(row => (row.getTimestamp("datetime_"), fromUDTValue(row.getUDTValue(colname)))).toIterable

      val value = dataAggregation.aggAggregation(results)
      new AggUpdate(update.metric, update.tagset, timeAggregator.name, datetime, update.`type`, value.asJson(), update.aggregation)
   }

   final def asRawUdtValue(value: JsValue): UDTValue = asRawUdtValue(fromJson(value))
   final def asAggUdtValue(value: JsValue): UDTValue = asAggUdtValue(fromJson(value))
}

object DataTypeSupport
{
   // TODO : replace it by a list traversal
   private def inferSupport(`type`: String) = `type` match {
      case "tdouble" => DoubleSupport
      case "tlong" => LongSupport
      case "date_double" => DateDoubleSupport
   }

   def inferSupport(update: RawUpdate): DataTypeSupport[_] = inferSupport(update.`type`)

   def inferSupport(update: AggUpdate): DataTypeSupport[_] = inferSupport(update.`type`)
}
