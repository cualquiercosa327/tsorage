package be.cetic.tsorage.processor

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.datastax.driver.core.{ConsistencyLevel, Session}
import com.datastax.driver.core.querybuilder.QueryBuilder.{bindMarker, insertInto}
import com.typesafe.scalalogging.LazyLogging

import scala.jdk.CollectionConverters._

object DAO extends LazyLogging
{
   val datetimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

   def colName(name: String) = s""""$name""""
   def colValue(value: String)= s"""'${value.replace("'", "''")}'"""
   def tagAsClause(key: String, value: String) = s"(${colName(key)} = ${colValue(value)} )"
   def tagsetAsClause(tagset: Map[String, String]) = tagset match {
      case m:Map[String, String] if m.isEmpty => ""
      case _ => tagset.iterator.map(entry => tagAsClause(entry._1, entry._2)).mkString(" AND ", " AND ", "")
   }

   /**
     * Creates the query for retrieving the raw values corresponding of a shunk, over a specified shard
     * @param metric    The metric characterizing the time series
     * @param shard     The specified shard
     * @param shunkStart   The lower bound of the shunk)
     * @param shunkEnd     The upper bound of the shunk
     * @param tagset       The tagset characterizing the time series
     * @return A string representing a CQL query corresponding to the specified shunk
     */
   def getRawShunkValues(metric: String, shard: String, shunkStart: LocalDateTime, shunkEnd: LocalDateTime, tagset: Map[String, String]) =
   {
      /*
       * TODO : Datastax Query Builder replaces any string variable by "?" instead of the actual string value.
       *  Investigate and use the builder instead of this function.
       */

      val query =
         s"""
            | SELECT datetime, value
            | FROM tsorage_raw.numeric
            | WHERE
            |   (metric = '${metric}') AND
            |   (shard = '${shard}') AND
            |   (datetime > '${shunkStart.format(datetimeFormatter)}') AND
            |   (datetime <= '${shunkEnd.format(datetimeFormatter)}')
            |   ${tagsetAsClause(tagset)}
            |
           | ALLOW FILTERING;
         """.stripMargin

      query
   }

   /**
     * Creates the query for retrieving the aggregated values corresponding of a shunk, over a specified shard
     * @param metric    The metric characterizing the time series
     * @param shard     The specified shard
     * @param interval  The aggregation interval to retrieve
     * @param aggregator   The aggregation function to use
     * @param shunkStart   The lower bound of the shunk)
     * @param shunkEnd     The upper bound of the shunk
     * @param tagset       The tagset characterizing the time series
     * @return A string representing a CQL query corresponding to the specified shunk
     */
   def getAggShunkValues(
                           metric: String,
                           shard: String,
                           interval: String,
                           aggregator: String,
                           shunkStart: LocalDateTime,
                           shunkEnd: LocalDateTime,
                           tagset: Map[String, String]
                        ) =
   {
      /*
       * TODO : Datastax Query Builder replaces any string variable by "?" instead of the actual string value.
       *  Investigate and use the builder instead of this function.
       */

      val query =
         s"""
            | SELECT datetime, value
            | FROM tsorage_agg.numeric
            | WHERE
            |   (metric = '${metric}') AND
            |   (shard = '${shard}') AND
            |   (interval = '${interval}') AND
            |   (aggregator = '${aggregator}') AND
            |   (datetime > '${shunkStart.format(datetimeFormatter)}') AND
            |   (datetime <= '${shunkEnd.format(datetimeFormatter)}')
            |   ${tagsetAsClause(tagset)}
            |
           | ALLOW FILTERING;
         """.stripMargin

      query
   }

   /**
     * Synchronously submits a raw value in the rax table.
     * @param session
     * @param metric
     * @param shard
     * @param tagset
     * @param datetime
     * @param value
     */
   def submitValue(
                     session: Session,
                     metric: String,
                     shard: String,
                     tagset: Map[String, String],
                     datetime: LocalDateTime,
                     value: Float): Unit =
   {
      val statement = insertInto("tsorage_raw", "numeric")
         .value("metric", metric)
         .value("shard", shard)
         .values(tagset.keys.toList.asJava, tagset.asInstanceOf[Map[String, AnyRef]].values.toList.asJava)
         .value("datetime", datetime.format(datetimeFormatter))
         .value("value", value)
         .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

      logger.info(s"DAO submits value ${statement}")
      session.execute(statement.toString)
   }

   /**
     * Synchronously submits a value in the aggregated table.
     * @param session
     * @param metric
     * @param shard
     * @param period
     * @param aggregator
     * @param tagset
     * @param datetime
     * @param value
     */
   def submitValue(
                     session: Session,
                     metric: String,
                     shard: String,
                     period: String,
                     aggregator: String,
                     tagset: Map[String, String],
                     datetime: LocalDateTime,
                     value: Float): Unit =
   {
      val statement = insertInto("tsorage_agg", "numeric")
         .value("metric", metric)
         .value("shard", shard)
         .value("interval", period)
         .value("aggregator", aggregator)
         .values(tagset.keys.toList.asJava, tagset.asInstanceOf[Map[String, AnyRef]].values.toList.asJava)
         .value("datetime", datetime.format(datetimeFormatter))
         .value("value", value)
         .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

      logger.info(s"DAO submits value ${statement}")

      session.execute(statement.toString)
   }
}
