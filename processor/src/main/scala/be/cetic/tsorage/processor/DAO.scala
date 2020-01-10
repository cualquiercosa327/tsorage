package be.cetic.tsorage.processor

import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneOffset}

import be.cetic.tsorage.common.TimeSeries
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.datatype.DataTypeSupport
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.{BoundStatement, ConsistencyLevel, PreparedStatement}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._

object DAO extends LazyLogging with TimeFormatHelper
{
   def colName(name: String) = s""""$name""""
   def colValue(value: String)= s"""'${value.replace("'", "''")}'"""
   def tagAsClause(key: String, value: String) = s"(${colName(key)} = ${colValue(value)} )"

   private val rawKeyspace = ProcessorConfig.rawKS
   private val aggKeyspace = ProcessorConfig.aggKS

   def tagsetAsClause(tagset: Map[String, String]) = tagset match {
      case m:Map[String, String] if m.isEmpty => ""
      case _ => tagset.iterator.map(entry => tagAsClause(entry._1, entry._2)).mkString(" AND ", " AND ", "")
   }

   // type -> get raw statements
   private var rawShunkStatementCache: Map[String, PreparedStatement] = Map()

   // type -> get agg statements
   private var aggShunkStatementCache: Map[String, PreparedStatement] = Map()

   private def prepareRawShunkStatement(`type`: String): PreparedStatement =
   {
      if(rawShunkStatementCache contains `type`) rawShunkStatementCache(`type`)
      else
      {
         val support = DataTypeSupport.inferSupport(`type`)
         val statement = Cassandra.session.prepare(
            QueryBuilder.select("datetime", support.colname)
            .from(rawKeyspace, "observations")
            .where(QueryBuilder.eq("metric", QueryBuilder.bindMarker("metric")))
            .and(QueryBuilder.eq("shard", QueryBuilder.bindMarker("shard")))
            .and(QueryBuilder.eq("tagset", QueryBuilder.bindMarker("tagset")))
            .and(QueryBuilder.gt("datetime", QueryBuilder.bindMarker("min_dt")))
            .and(QueryBuilder.lte("datetime", QueryBuilder.bindMarker("max_dt")))
         ).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

         rawShunkStatementCache = rawShunkStatementCache ++ Map(`type` -> statement)

         statement
      }
   }

   private def prepareAggShunkStatement(`type`: String): PreparedStatement =
   {
      if(aggShunkStatementCache contains `type`) aggShunkStatementCache(`type`)
      else
      {
         val support = DataTypeSupport.inferSupport(`type`)
         val statement = Cassandra.session.prepare(
            QueryBuilder.select("datetime", support.colname)
               .from(aggKeyspace, "observations")
               .where(QueryBuilder.eq("metric", QueryBuilder.bindMarker("metric")))
               .and(QueryBuilder.eq("shard", QueryBuilder.bindMarker("shard")))
               .and(QueryBuilder.eq("interval", QueryBuilder.bindMarker("interval")))
               .and(QueryBuilder.eq("aggregator", QueryBuilder.bindMarker("aggregator")))
               .and(QueryBuilder.eq("tagset", QueryBuilder.bindMarker("tagset")))
               .and(QueryBuilder.gt("datetime", QueryBuilder.bindMarker("min_dt")))
               .and(QueryBuilder.lte("datetime", QueryBuilder.bindMarker("max_dt")))
         ).setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

         aggShunkStatementCache = aggShunkStatementCache ++ Map(`type` -> statement)

         statement
      }
   }

   /**
     * Creates the query statement for retrieving the raw values corresponding of a shunk, over a specified shard
     * @param ts    The time series
     * @param shard     The specified shard
     * @param shunkStart   The lower bound of the shunk)
     * @param shunkEnd     The upper bound of the shunk
     * @return A query statement corresponding to the specified shunk
     */
   def getRawShunkValues(
                           ts: TimeSeries,
                           shard: String,
                           shunkStart: LocalDateTime,
                           shunkEnd: LocalDateTime,
                           `type`: String
                        ): BoundStatement =
   {
      prepareRawShunkStatement(`type`)
         .bind()
         .setString("metric", ts.metric)
         .setString("shard", shard)
         .setMap("tagset", ts.tagset.asJava)
         .setTimestamp("min_dt", Timestamp.from(shunkStart.atOffset(ZoneOffset.UTC).toInstant))
         .setTimestamp("max_dt", Timestamp.from(shunkEnd.atOffset(ZoneOffset.UTC).toInstant))
   }

   /**
     * Creates the query statement for retrieving the aggregated values corresponding of a shunk, over a specified shard
     * @param ts    The time series
     * @param shard     The specified shard
     * @param interval  The aggregation interval to retrieve
     * @param aggregator   The aggregation function to use
     * @param shunkStart   The lower bound of the shunk)
     * @param shunkEnd     The upper bound of the shunk
     * @return A query statement corresponding to the specified shunk
     */
   def getAggShunkValues(
                           ts: TimeSeries,
                           shard: String,
                           interval: String,
                           aggregator: String,
                           `type`: String,
                           shunkStart: LocalDateTime,
                           shunkEnd: LocalDateTime
                        ): BoundStatement =
   {
      prepareAggShunkStatement(`type`)
         .bind()
         .setString("metric", ts.metric)
         .setString("shard", shard)
         .setMap("tagset", ts.tagset.asJava)
         .setString("interval", interval)
         .setString("aggregator", aggregator)
         .setTimestamp("min_dt", Timestamp.from(shunkStart.atOffset(ZoneOffset.UTC).toInstant))
         .setTimestamp("max_dt", Timestamp.from(shunkEnd.atOffset(ZoneOffset.UTC).toInstant))
   }
}
