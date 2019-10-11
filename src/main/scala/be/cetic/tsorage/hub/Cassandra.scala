package be.cetic.tsorage.hub

import com.datastax.driver.core.querybuilder.{BindMarker, QueryBuilder}
import com.datastax.driver.core.querybuilder.QueryBuilder._
import com.datastax.driver.core.{Cluster, ConsistencyLevel, Row, Session}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._

/**
 * An access to the Cassandra cluster
 */
object Cassandra extends LazyLogging
{
   private val conf = ConfigFactory.load("hub.conf")

   private val cassandraHost = conf.getString("cassandra.host")
   private val cassandraPort = conf.getInt("cassandra.port")

   private val keyspace = conf.getString("cassandra.keyspaces.other")

   private val session: Session = Cluster.builder
      .addContactPoint(cassandraHost)
      .withPort(cassandraPort)
      .withoutJMXReporting()
      .build
      .connect()

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

   /**
    * Updates a subset of the static tags associated with a metric.
    * @param metric  The metric to update.
    * @param tags    The static tags to update.
    */
   def updateStaticTagset(metric: String, tags: Map[String, String]) = {
      tags.foreach(tag => {
         val statement = QueryBuilder.update(keyspace, "static_tagset")
            .`with`(QueryBuilder.set("tagvalue", tag._2))
            .where(QueryBuilder.eq("metric", metric))
            .and(QueryBuilder.eq("tagname", tag._1))
            .setConsistencyLevel(ConsistencyLevel.ONE)

         session.executeAsync(statement)
      })
   }

   /**
    * Replaces a static tagset by a new one. Any previous static tag is deleted.
    *
    * @param metric  The metric associated with the tagset.
    * @param tagset  The new tagset associated with the metric.
    */
   def setStaticTagset(metric: String, tagset: Map[String, String]): Unit =
   {
      val deleteStatement = QueryBuilder.delete()
         .from(keyspace, "static_tagset")
         .where(QueryBuilder.eq("metric", metric))
         .setConsistencyLevel(ConsistencyLevel.ONE)

      session.execute(deleteStatement)

      updateStaticTagset(metric, tagset)
   }

   /**
    * Retrieves the names of all the stored metrics.
    * For the moment, this list is limited to the metrics for which a static tagset has been defined, even if this tagset is empty. In the future, this limitation may be removed.
    * @return  The names of all the metrics having a defined static tagset.
    */
   def getAllMetrics() =
   {
      val statement = QueryBuilder
         .select("metric")
         .distinct()
         .from(keyspace, "static_statement")
         .setConsistencyLevel(ConsistencyLevel.ONE)

      session.execute(statement).iterator().asScala.map(row => row.getString("metric"))
   }

   /**
    * @param tagname    The name of a static tag.
    * @param tagvalue   The value of a static tag.
    * @return  The names of all the metrics having the specified static tag.
    */
   def getMetricsWithStaticTag(tagname: String, tagvalue: String) =
   {
      val statement = QueryBuilder.select("metric")
         .from(keyspace, "reverse_static_tagset")
         .where(QueryBuilder.eq("tagname", tagname))
         .and(QueryBuilder.eq("tagvalue", tagvalue))
         .setConsistencyLevel(ConsistencyLevel.ONE)

      session.execute(statement).asScala
         .map(row => row.getString("metric"))
         .toSet
   }

   /**
    * Retrieves the names of all the metrics having a given set of static tags.
    *
    * These names are collected by iteratively calculate the intersection of the metrics associated
    * with each of the tag set entries.
    *
    * @param tagset  The set of static tags a metric must be associated with in order to be returned.
    *                If an empty tagset is provided, all known metrics are retrieved.
    * @return The metrics having the specified set of static tags.
    * @see getAllMetrics
    */
   def getMetricsWithStaticTagset(tagset: Map[String, String]) = tagset match
   {
      case m: Map[String, String] if m.isEmpty => getAllMetrics()
      case _ => {
         val tags = tagset.toList
         val firstTag = tags.head
         val otherTags = tags.tail

         def merge(candidates: Set[String], tag: (String, String)) = {
            val otherCandidates = getMetricsWithStaticTag(tag._1, tag._2).toSet
            candidates.intersect(otherCandidates)
         }

         otherTags.foldLeft(getMetricsWithStaticTag(firstTag._1, firstTag._2).toSet)(merge)
      }
   }

   /**
    * Provides the list of values being associated with a static tag name.
    * @param tagname The name of a static tag.
    * @return  The values associated with tagname, as well as the metrics using the combined (tag name, tag value) as static tag.
    *          If the tag name is not in use, an empty set is retrieved.
    */
   def getStaticTagValues(tagname: String): Map[String, Set[String]] =
   {
      val statement = select("tagvalue", "metric")
         .from(keyspace, "reverse_static_tagset")
         .where(QueryBuilder.eq("tagname", tagname))

      session
         .execute(statement).asScala
         .map(row => (row.getString("tagvalue") -> row.getString("metric")))
         .groupBy(r => r._1)
         .mapValues(v => v.map(_._2).toSet)
   }
}
