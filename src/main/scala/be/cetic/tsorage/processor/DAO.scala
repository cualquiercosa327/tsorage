package be.cetic.tsorage.processor

import java.time.LocalDateTime

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
     * Creates the query for retrieving the raw values corresponding of a shunk, over a specified shard
     * @param metric    The metric characterizing the time series
     * @param shard     The specified shard
     * @param shunkStart   The lower bound of the shunk)
     * @param shunkEnd     The upper bound of the shunk
     * @param tagset       The tagset characterizing the time series
     * @return A string representing a CQL query corresponding to the specified shunk
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
      /*
       * TODO : Datastax Query Builder replaces any string variable by "?" instead of the actual string value.
       *  Investigate and use the builder instead of this function.
       */

      val query =
         s"""
            | SELECT datetime_, ${colname}
            | FROM ${rawKeyspace}.observations
            | WHERE
            |   (metric_ = '${metric}') AND
            |   (shard_ = '${shard}') AND
            |   (datetime_ > '${formatLDT(shunkStart)}') AND
            |   (datetime_ <= '${formatLDT(shunkEnd)}')
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
                           colname: String,
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
            | SELECT datetime_, ${colname}
            | FROM ${aggKeyspace}.observations
            | WHERE
            |   (metric_ = '${metric}') AND
            |   (shard_ = '${shard}') AND
            |   (interval_ = '${interval}') AND
            |   (aggregator_ = '${aggregator}') AND
            |   (datetime_ > '${formatLDT(shunkStart)}') AND
            |   (datetime_ <= '${formatLDT(shunkEnd)}')
            |   ${tagsetAsClause(tagset)}
            |
           | ALLOW FILTERING;
         """.stripMargin

      query
   }
}
