package be.cetic.tsorage.hub.filter

import java.time.LocalDateTime

import be.cetic.tsorage.common.Cassandra
import be.cetic.tsorage.common.Cassandra.{keyspace, logger, session}
import be.cetic.tsorage.hub.metric.QueryDateRange
import com.datastax.driver.core.{ConsistencyLevel, ResultSetFuture, Session, Statement}
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import collection.JavaConverters._

/**
 * An entity for manipulating metric collections based on specified predicates.
 */
case class MetricManager(session: Session, conf: Config) extends LazyLogging
{
   private val keyspace = conf.getString("cassandra.keyspaces.other")

   private val allMetricsStaticStatement = session.prepare(
      QueryBuilder
         .select("metric")
         .distinct()
         .from(keyspace, "static_tagset")
   ).setConsistencyLevel(ConsistencyLevel.ONE)

   private val allMetricsDynamicStatement = session.prepare(
      QueryBuilder
         .select("metric")
         .distinct()
         .from(keyspace, "dynamic_tagset")
   ).setConsistencyLevel(ConsistencyLevel.ONE)

   private val metricTagStaticStatement = session.prepare(
      QueryBuilder
         .select("metric")
         .from(keyspace, "reverse_static_tagset")
         .where(QueryBuilder.eq("tagname", QueryBuilder.bindMarker("tagname")))
         .and(QueryBuilder.eq("tagvalue", QueryBuilder.bindMarker("tagvalue")))
   ).setConsistencyLevel(ConsistencyLevel.ONE)

   private val metricTagDynamicStatement = session.prepare(
      QueryBuilder
         .select("metric")
         .from(keyspace, "reverse_dynamic_tagset")
         .where(QueryBuilder.eq("tagname", QueryBuilder.bindMarker("tagname")))
         .and(QueryBuilder.eq("tagvalue", QueryBuilder.bindMarker("tagvalue")))
   ).setConsistencyLevel(ConsistencyLevel.ONE)

   private val shardedMetricTagDynamicStatement = session.prepare(
      QueryBuilder
         .select("metric")
         .from(keyspace, "reverse_sharded_dynamic_tagset")
         .where(QueryBuilder.eq("shard", QueryBuilder.bindMarker("shard")))
         .and(QueryBuilder.eq("tagname", QueryBuilder.bindMarker("tagname")))
         .and(QueryBuilder.eq("tagvalue", QueryBuilder.bindMarker("tagvalue")))
   ).setConsistencyLevel(ConsistencyLevel.LOCAL_ONE)

   private val metricExistingTagStaticStatement = session.prepare(
      QueryBuilder
         .select("metric")
         .from(keyspace, "reverse_static_tagset")
         .where(QueryBuilder.eq("tagname", QueryBuilder.bindMarker("tagname")))
   ).setConsistencyLevel(ConsistencyLevel.ONE)

   private val metricExistingTagDynamicStatement = session.prepare(
      QueryBuilder
         .select("metric")
         .from(keyspace, "reverse_dynamic_tagset")
         .where(QueryBuilder.eq("tagname", QueryBuilder.bindMarker("tagname")))
   ).setConsistencyLevel(ConsistencyLevel.ONE)

   private val shardedMetricExistingTagDynamicStatement = session.prepare(
      QueryBuilder
         .select("metric")
         .from(keyspace, "reverse_sharded_dynamic_tagset")
         .where(QueryBuilder.eq("shard", QueryBuilder.bindMarker("shard")))
         .and(QueryBuilder.eq("tagname", QueryBuilder.bindMarker("tagname")))
   ).setConsistencyLevel(ConsistencyLevel.LOCAL_ONE)

   /**
    * Retrieves the names of all the stored metrics.
    * For the moment, this list is limited to the metrics for which a static tagset has been defined, even if this tagset is empty. In the future, this limitation may be removed.
    * @return  The names of all the metrics having a defined static tagset.
    */
   def getAllMetrics(): Set[String] =
   {
      val statementA = allMetricsStaticStatement.bind()
      val statementB = allMetricsDynamicStatement.bind()

      mergeFutureResultSets(statementA, statementB)
   }

   /**
    * @param tagname    The name of a tag.
    * @param tagvalue   The value associated with the tag name.
    * @return  All the metrics having the specified tag.
    */
   def getMetricWithTag(tagname: String, tagvalue: String): Set[String] =
   {
      val statementA = metricTagStaticStatement.bind()
         .setString("tagname", tagname)
         .setString("tagvalue", tagvalue)

      val statementB =  metricTagDynamicStatement.bind()
         .setString("tagname", tagname)
         .setString("tagvalue", tagvalue)

      mergeFutureResultSets(statementA, statementB)
   }

   /**
    * @param tagname    The name of a tag.
    * @param tagvalue   The value associated with the tag name.
    * @param start      The beginning of a time range.
    * @param end        The end of a time range.
    * @return           All the metrics having the specified tag. Because dynamic tagsets are recorded by shard,
    *                   some retrieved metrics may have no relevant values in the specified time range.
    */
   def getMetricWithTag(tagname: String, tagvalue: String, start: LocalDateTime, end: LocalDateTime): Set[String] =
   {
      val shards = Cassandra.sharder.shards(start, end)

      val staticMetrics = session
         .execute(metricExistingTagStaticStatement.bind().setString("tagname", tagname))
         .asScala
         .map(row => row.getString("metric")).toSet

      val shardMetrics = shards.par.map(shard => session.execute(
         shardedMetricTagDynamicStatement
            .bind()
            .setString("shard", shard)
            .setString("tagname", tagname)
            .setString("tagvalue", tagvalue)
         ).asScala.map(row => row.getString("metric")).toSet
      ).reduce( (m1, m2) => m1 ++ m2)

      staticMetrics ++ shardMetrics
   }

   def mergeFutureResultSets(a: Statement, b: Statement): Set[String] =
   {
      val aMetrics = session.execute(a).asScala.map(row => row.getString("metric"))
      val bMetrics = session.execute(b).asScala.map(row => row.getString("metric"))

      (aMetrics ++ bMetrics).toSet
   }

   /**
    * @param tagname The name of a tag
    * @return  All the metrics having a tag with the specified name.
    */
   def getMetricWithExistingTagname(tagname: String): Set[String] =
   {
      val statementA = metricExistingTagStaticStatement.bind().setString("tagname", tagname)
      val statementB = metricExistingTagDynamicStatement.bind().setString("tagname", tagname)

      mergeFutureResultSets(statementA, statementB)
   }

   /**
    * @param tagname The name of a tag
    * @param start   The beginning of a time range.
    * @param end     The end of a time range.
    * @return        All the metrics having a tag with the specified name in the specified time range.
    *                Because dynamic tagsets are recorded by shard, some retrieved metrics may have no
    *                relevant values in the specified time range.
    */
   def getMetricWithExistingTagname(tagname: String, start: LocalDateTime, end: LocalDateTime): Set[String] =
   {
      val staticMetrics = session
         .execute(metricExistingTagStaticStatement.bind().setString("tagname", tagname))
         .asScala
         .map(row => row.getString("metric")).toSet

      val shards = Cassandra.sharder.shards(start, end)

      val shardMetrics = shards.par.map(shard =>
         session.execute(
            shardedMetricExistingTagDynamicStatement
               .bind()
               .setString("shard", shard)
               .setString("tagname", tagname)
         ).asScala
         .map(row => row.getString("metric"))
         .toSet
      ).reduce( (m1, m2) => m1 ++ m2)

      staticMetrics ++ shardMetrics
   }


   /**
    * Retrieve all the metrics matching a given filter.
    * @param filter  A predicate all returned metrics must match.
    * @param range   An optional time range
    * @return All the metrics matching the specified filter, potentially in the specified time range.
    */
   def getMetrics(filter: Filter, range: Option[QueryDateRange]): Set[String] =
   {
      lazy val allMetrics: Set[String] = getAllMetrics()

      def recGetMetrics(f: Filter): Set[String] = f match {
         case TagFilter(key, value) => range match {
            case None => getMetricWithTag(key, value)
            case Some(QueryDateRange(start, end)) => getMetricWithTag(key, value, start, end)
         }

         case TagExist(key) => range match {
            case None => getMetricWithExistingTagname(key)
            case Some(QueryDateRange(start, end)) => getMetricWithExistingTagname(key, start, end)
         }

         case And(a, b) => recGetMetrics(a) intersect recGetMetrics(b)
         case Or(a, b) =>  recGetMetrics(a) union recGetMetrics(b)

         case Not(Not(f2)) => recGetMetrics(f2)
         case Not(f2 : TagFilter) => allMetrics -- recGetMetrics(f2)
         case Not(f2 : TagExist) => allMetrics -- recGetMetrics(f2)

         // De Morgan
         case Not(And(a,b)) => recGetMetrics(Or(Not(a), Not(b)))
         case Not(Or(a,b)) => recGetMetrics(And(Not(a), Not(b)))
      }

      recGetMetrics(filter)
   }

   /**
    * @param metric  A metric.
    * @return  The static tagset associated with the metric.
    *          An empty map is returned if there is no static tagset associated with the metric.
    */
   def getStaticTagset(metric: String): Map[String, String] =
   {
      val statement = QueryBuilder.select("tagname", "tagvalue")
         .from(keyspace, "static_tagset")
         .where(QueryBuilder.eq("metric", metric))

      val result = session
         .execute(statement)
         .iterator().asScala
         .map(row => row.getString("tagname") -> row.getString("tagvalue"))
         .toMap

      logger.debug(s"Static tagset retrieved for ${metric}: ${result}")

      result
   }
}
