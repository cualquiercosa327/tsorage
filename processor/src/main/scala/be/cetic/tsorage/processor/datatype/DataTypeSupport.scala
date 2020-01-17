package be.cetic.tsorage.processor.datatype

import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.Date

import be.cetic.tsorage.common.FutureManager
import be.cetic.tsorage.common.messaging.AggUpdate
import be.cetic.tsorage.processor.aggregator.data.DataAggregation
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.update.TimeAggregatorRawUpdate
import be.cetic.tsorage.processor.{DAO, ProcessorConfig}
import com.datastax.driver.core.{Row, UDTValue, UserType}
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
   val colname: String
   val `type`: String

   protected lazy val rawUDTType : UserType = Cassandra.session
      .getCluster
      .getMetadata
      .getKeyspace(ProcessorConfig.rawKS)
      .getUserType(s"${`type`}")

   protected lazy val aggUDTType : UserType = Cassandra.session
      .getCluster
      .getMetadata
      .getKeyspace(ProcessorConfig.aggKS)
      .getUserType(s"${`type`}")

   def asJson(value: T): JsValue
   def fromJson(value: JsValue): T

   private def date2ldt(d: Date): LocalDateTime = Instant.ofEpochMilli(d.getTime()).atZone(ZoneId.of("GMT")).toLocalDateTime()

   /**
     * Converts a value into a Cassandra UDT Value
     * @param value The value to convert
     * @return The UDTValue representing the value
     */
   def asRawUdtValue(value: T): UDTValue
   def asAggUdtValue(value: T): UDTValue

   def fromUDTValue(value: UDTValue): T

   def udt2json(value: UDTValue): JsValue = asJson(fromUDTValue(value))

   def getRawValues(
                      update: TimeAggregatorRawUpdate,
                      shunkStart: LocalDateTime,
                      shunkEnd: LocalDateTime)
                   (implicit ec: ExecutionContextExecutor): Future[List[(LocalDateTime, JsValue)]] = {
      val coveringShards = Cassandra.sharder.shards(shunkStart, shunkEnd)

      val queryStatements = coveringShards.map(shard =>
         DAO.getRawShunkValues(update.ts, shard, shunkStart, shunkEnd, update.`type`)
      )

      Future.reduceLeft(queryStatements.map(statement =>
         resultSetFutureToFutureResultSet(Cassandra.session.executeAsync(statement))
            .map(rs => rs.asScala.map(row => (date2ldt(row.getTimestamp("datetime")), udt2json(row.getUDTValue(colname))))
            ))
      )( (l1, l2) => l1 ++ l2)
         .map(_.toList)
   }

   /**
    * Retrieve all the aggregated values that relate to a shunk to be updated.
    * @param au   The aggupdate that causes the update.
    * @param timeAggregator   The next level of aggregation.
    * @return
    */
   def getHistoryValues(au: AggUpdate, timeAggregator: TimeAggregator)(implicit ec: ExecutionContextExecutor):
   Future[List[(LocalDateTime, JsValue)]] =
   {
      val datetime = timeAggregator.shunk(au.datetime)
      val (start, end) = timeAggregator.range(datetime)
      val coveringShards = Cassandra.sharder.shards(start, end)

      val statements = coveringShards.map(shard => DAO.getAggShunkValues(
         au.ts,
         shard,
         timeAggregator.previousName,
         au.aggregation,
         au.`type`,
         start,
         end
      ))

      val futures = statements
         .map(statement => resultSetFutureToFutureResultSet(Cassandra.session.executeAsync(statement)))

      Future
         .foldLeft(futures)(List[Row]())( (l1, l2) => l1 ++ l2.all().asScala)
         .map(result => result.map(row => (DataTypeSupport.date2ldt(row.getTimestamp("datetime")), asJson(fromUDTValue(row.getUDTValue(colname))))))
   }

   final def asRawUdtValue(value: JsValue): UDTValue = asRawUdtValue(fromJson(value))
   final def asAggUdtValue(value: JsValue): UDTValue = asAggUdtValue(fromJson(value))
}

object DataTypeSupport
{
   val availableSupports: Map[String, DataTypeSupport[_]] = List(
      DoubleSupport,
      LongSupport,
      DateDoubleSupport,
      Position2DSupport
   ).map(support => support.`type` -> support).toMap

   def inferSupport(`type`: String) = availableSupports(`type`)

   def inferSupport(update: TimeAggregatorRawUpdate): DataTypeSupport[_] = inferSupport(update.`type`)

   def inferSupport(update: AggUpdate): DataTypeSupport[_] = inferSupport(update.`type`)

   def date2ldt(date: Date): LocalDateTime = Instant
      .ofEpochMilli(date.getTime())
      .atZone(ZoneId.of("GMT"))
      .toLocalDateTime()
}
