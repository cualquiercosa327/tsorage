package be.cetic.tsorage.processor

import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneOffset}

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.typesafe.scalalogging.LazyLogging

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

   /**
     * Creates the query statement for retrieving the raw values corresponding of a shunk, over a specified shard
     * @param metric    The metric characterizing the time series
     * @param shard     The specified shard
     * @param shunkStart   The lower bound of the shunk)
     * @param shunkEnd     The upper bound of the shunk
     * @param tagset       The tagset characterizing the time series
     * @return A query statement corresponding to the specified shunk
     */
   def getRawShunkValues(
                           metric: String,
                           shard: String,
                           shunkStart: LocalDateTime,
                           shunkEnd: LocalDateTime,
                           tagset: Map[String, String],
                           colname: String
                        ) =
   {
      val base = QueryBuilder
         .select("datetime_", colname)
         .from(rawKeyspace, "observations")
         .where(QueryBuilder.eq("metric_", metric))
         .and(QueryBuilder.eq("shard_", shard))
         .and(QueryBuilder.gt("datetime_", Timestamp.from(shunkStart.atOffset(ZoneOffset.UTC).toInstant)))
         .and(QueryBuilder.lte("datetime_", Timestamp.from(shunkEnd.atOffset(ZoneOffset.UTC).toInstant)))

      tagset.foldLeft(base)( (b,tag) => b.and(QueryBuilder.eq(tag._1, tag._2)))
            .allowFiltering()
   }

   /**
     * Creates the query statement for retrieving the aggregated values corresponding of a shunk, over a specified shard
     * @param metric    The metric characterizing the time series
     * @param shard     The specified shard
     * @param interval  The aggregation interval to retrieve
     * @param aggregator   The aggregation function to use
     * @param shunkStart   The lower bound of the shunk)
     * @param shunkEnd     The upper bound of the shunk
     * @param tagset       The tagset characterizing the time series
     * @return A query statement corresponding to the specified shunk
     */
   def getAggShunkValues(
                           metric: String,
                           shard: String,
                           interval: String,
                           aggregator: String,
                           colname: String,
                           shunkStart: LocalDateTime,
                           shunkEnd: LocalDateTime,
                           tagset: Map[String, String]
                        ) =
   {
      val base = QueryBuilder
         .select("datetime_", colname)
         .from(aggKeyspace, "observations")
         .where(QueryBuilder.eq("metric_", metric))
         .and(QueryBuilder.eq("shard_", shard))
         .and(QueryBuilder.eq("interval_", interval))
         .and(QueryBuilder.eq("aggregator_", aggregator))
         .and(QueryBuilder.gt("datetime_", Timestamp.from(shunkStart.atOffset(ZoneOffset.UTC).toInstant)))
         .and(QueryBuilder.lte("datetime_", Timestamp.from(shunkEnd.atOffset(ZoneOffset.UTC).toInstant)))

      tagset.foldLeft(base)( (b,tag) => b.and(QueryBuilder.eq(tag._1, tag._2)))
         .allowFiltering()
   }
}
