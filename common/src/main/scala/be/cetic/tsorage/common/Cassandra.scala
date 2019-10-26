package be.cetic.tsorage.common

import java.time.LocalDateTime

import be.cetic.tsorage.common.sharder.Sharder
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.select
import com.datastax.driver.core.{Cluster, ConsistencyLevel, Session}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import collection.JavaConverters._


/**
 * An access to the Cassandra cluster
 */
object Cassandra extends LazyLogging
{
   private val conf = ConfigFactory.load("common.conf")

   private val cassandraHost = conf.getString("cassandra.host")
   private val cassandraPort = conf.getInt("cassandra.port")

   private val keyspace = conf.getString("cassandra.keyspaces.other")

   val session: Session = Cluster.builder
      .addContactPoint(cassandraHost)
      .withPort(cassandraPort)
      .withoutJMXReporting()
      .build
      .connect()

   val sharder = Sharder(conf.getString("sharder"))

   private val getDynamicTagsetStatement = session.prepare(
      QueryBuilder.select("tagname", "tagvalue")
         .from(keyspace, "dynamic_tagset")
         .where(QueryBuilder.eq("metric", QueryBuilder.bindMarker("metric")))
   )

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
         .setConsistencyLevel(ConsistencyLevel.ONE)

      session
         .execute(statement).asScala
         .map(row => (row.getString("tagvalue") -> row.getString("metric")))
         .groupBy(r => r._1)
         .mapValues(v => v.map(_._2).toSet)
   }

   /**
    * @param metric  A metric.
    * @param shards  A set of shards.
    * @return  All the dynamic tagsets associated with the metric during the specified shards.
    */
   def getDynamicTagset(metric: String, shards: Set[String]): Set[(String, String)] =
   {
      val statement = getDynamicTagsetStatement.bind()
            .setString("metric", metric)
            .setSet("shards", shards.asJava)
            .setConsistencyLevel(ConsistencyLevel.ONE)

      session
         .execute(statement).asScala
         .map(row => (row.getString("tagname"), row.getString("tagvalue")))
         .toSet
   }
}
