package be.cetic.tsorage.processor.datatype

import java.time.LocalDateTime
import java.util.Date

import akka.NotUsed
import akka.stream.scaladsl.{Flow, GraphDSL}
import be.cetic.tsorage.processor.aggregator.data.DataAggregation
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.flow.FutureManager
import be.cetic.tsorage.processor.update.{AggUpdate, RawUpdate, TimeAggregatorRawUpdate}
import be.cetic.tsorage.processor.{DAO, ProcessorConfig}
import com.datastax.driver.core.{ConsistencyLevel, SimpleStatement, UDTValue, UserType}
import com.typesafe.scalalogging.LazyLogging
import spray.json._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * A support is an entity that helps in managing a particular type of time series value.
  * @tparam T The type of the time series
  */
abstract class DataTypeSupport[T] extends LazyLogging with FutureManager
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

   def getRawValues(
                      update: TimeAggregatorRawUpdate,
                      shunkStart: LocalDateTime,
                      shunkEnd: LocalDateTime)
                   (implicit ec: ExecutionContextExecutor): Future[Iterable[(Date, T)]] = {
      val coveringShards = Cassandra.sharder.shards(shunkStart, shunkEnd)

      val queryStatements = coveringShards.map(shard =>
         DAO.getRawShunkValues(update.ts, shard, shunkStart, shunkEnd, update.`type`)
      )

      Future.reduceLeft(queryStatements.map(statement =>
         resultSetFutureToFutureResultSet(Cassandra.session.executeAsync(statement))
            .map(rs => rs.asScala.map(row => (row.getTimestamp("datetime"), fromUDTValue(row.getUDTValue(colname))))
            ))
      )( (l1, l2) => l1 ++ l2)
   }


   /**
     * Transforms a raw update into a list of aggregated updates.
     * @param update The raw update to transform.
     * @param timeAggregator  The time aggregator involved in this transformation.
     * @return A list of aggregated updates.
     */
   def prepareAggUpdate(
                          update: TimeAggregatorRawUpdate,
                          timeAggregator: TimeAggregator)
                       (implicit ec: ExecutionContextExecutor)
   : Future[List[AggUpdate]] =
   {
      val datetime = update.shunk
      val (shunkStart: LocalDateTime, shunkEnd: LocalDateTime) = timeAggregator.range(datetime)
      val results = getRawValues(update, shunkStart: LocalDateTime, shunkEnd: LocalDateTime)

      val aggs = rawAggregations.map(agg => {
            results.map(events => List(agg.rawAggregation(events).asAggUpdate(update, timeAggregator, datetime, agg.name)))
      })

      Future.reduceLeft(aggs)(_++_)
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

      val statements = coveringShards.map(shard => DAO.getAggShunkValues(
         update.ts,
         shard,
         timeAggregator.previousName,
         update.aggregation,
         update.`type`,
         shunkStart,
         shunkEnd
      ))
      // logger.info(s"QUERIES: ${queries.mkString(" ; ")}")

      val results = statements.par
         .map(statement => Cassandra.session.execute(statement).all().asScala)
         .reduce((l1, l2) => l1 ++ l2)
         .map(row => (row.getTimestamp("datetime"), fromUDTValue(row.getUDTValue(colname)))).toIterable

      val value = dataAggregation.aggAggregation(results)
      new AggUpdate(update.ts, timeAggregator.name, datetime, update.`type`, value.asJson(), update.aggregation)
   }

   final def asRawUdtValue(value: JsValue): UDTValue = asRawUdtValue(fromJson(value))
   final def asAggUdtValue(value: JsValue): UDTValue = asAggUdtValue(fromJson(value))
}

object DataTypeSupport
{
   val availableSupports: Map[String, DataTypeSupport[_]] = List(
      DoubleSupport,
      LongSupport,
      DateDoubleSupport
   ).map(support => support.`type` -> support).toMap

   def inferSupport(`type`: String) = availableSupports(`type`)

   def inferSupport(update: RawUpdate): DataTypeSupport[_] = inferSupport(update.`type`)
   def inferSupport(update: TimeAggregatorRawUpdate): DataTypeSupport[_] = inferSupport(update.`type`)

   def inferSupport(update: AggUpdate): DataTypeSupport[_] = inferSupport(update.`type`)
}
