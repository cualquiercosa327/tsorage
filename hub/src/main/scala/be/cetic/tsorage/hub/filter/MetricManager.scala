package be.cetic.tsorage.hub.filter

import be.cetic.tsorage.common.FutureManager
import be.cetic.tsorage.hub.Cassandra
import be.cetic.tsorage.hub.metric.MetricSearchQuery
import com.datastax.driver.core.ConsistencyLevel
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._

/**
 * An entity for manipulating metric collections based on specified predicates.
 */
case class MetricManager(cassandra: Cassandra, conf: Config) extends LazyLogging with FutureManager
{
   private val keyspace = conf.getString("cassandra.keyspaces.other")

   private val session = cassandra.session

   private val metricwithStaticTagnameStatement = session.prepare(
      QueryBuilder
         .select("metric")
         .from(keyspace, "reverse_static_tagset")
         .where(QueryBuilder.eq("tagname", QueryBuilder.bindMarker("tagname")))
   ).setConsistencyLevel(ConsistencyLevel.ONE)

   private val metricwithStaticTagStatement = session.prepare(
      QueryBuilder
         .select("metric")
         .from(keyspace, "reverse_static_tagset")
         .where(QueryBuilder.eq("tagname", QueryBuilder.bindMarker("tagname")))
         .and(QueryBuilder.eq("tagvalue", QueryBuilder.bindMarker("tagvalue")))
   ).setConsistencyLevel(ConsistencyLevel.ONE)

   private val allMetricsFromStaticStatement = session.prepare(
      QueryBuilder
         .select("metric")
         .distinct()
         .from(keyspace, "static_tagset")
   ).setConsistencyLevel(ConsistencyLevel.ONE)

   private val allMetricsFromDynamicStatement = session.prepare(
      QueryBuilder
         .select("metric")
         .distinct()
         .from(keyspace, "dynamic_tagset")
   ).setConsistencyLevel(ConsistencyLevel.ONE)



   // =============================


   /**
    * @param tagname    The name of a tag.
    * @param tagvalue   The value associated with the tag name.
    * @return           All the metrics having the specified static tag.
    */
   def getMetricWithStaticTag(tagname: String, tagvalue: String): Set[Metric] =
   {
      session.execute(
            metricwithStaticTagStatement
               .bind()
               .setString("tagname", tagname)
               .setString("tagvalue", tagvalue)
         )
         .asScala
         .map(row => row.getString("metric")).toSet.map(m => Metric(m, session, conf))
   }

   /**
    * @param tagname The name of a tag
    * @return  All the metrics having a tag with the specified name.
    */
   def getMetricWithStaticTagname(tagname: String): Set[Metric] =
   {
      val statement = metricwithStaticTagnameStatement
         .bind()
         .setString("tagname", tagname)

      session.execute(statement)
         .asScala
         .map(row => row.getString("metric"))
         .toSet
         .map(m => Metric(m, session, conf))
   }

   /**
    * @return  All metrics available, globally.
    */
   def getAllMetrics(): Set[Metric] = {

      val fromStatic = session
         .execute(allMetricsFromStaticStatement.bind())
         .asScala
         .map(row => row.getString("metric"))
         .toSet

      val fromDynamic = session
         .execute(allMetricsFromDynamicStatement.bind())
         .asScala
         .map(row => row.getString("metric"))
         .toSet

      (fromStatic ++ fromDynamic).map(m => Metric(m, session, conf))
   }

   /**
    * @param query
    * @return All the metrics corresponding to query.
    */
   def getMetrics(query: MetricSearchQuery): Set[Metric] = query.filter match
   {
      case None => getAllMetrics()
      case Some(AllFilter) => getAllMetrics()
      case Some(filter) => {
         val timeSeriesManager = TimeSeriesManager(cassandra, conf)
         timeSeriesManager
            .getTimeSeries(TimeSeriesQuery(None, filter, query.range))
            .map(ts => Metric(ts.metric, session, conf))
      }
   }
}
