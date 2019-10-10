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
   private val conf = ConfigFactory.load("auth.conf")

   private val cassandraHost = conf.getString("cassandra.host")
   private val cassandraPort = conf.getInt("cassandra.port")

   private val keyspace = conf.getString("cassandra.keyspaces.other")

   private val session: Session = Cluster.builder
      .addContactPoint(cassandraHost)
      .withPort(cassandraPort)
      .withoutJMXReporting()
      .build
      .connect()

   private val removeReverseStaticTagsetStatement = session.prepare(
      QueryBuilder.update(keyspace, "reverse_tagset")
         .`with`(QueryBuilder.removeAll("metrics", QueryBuilder.bindMarker("metric")))
         .where(
            QueryBuilder.eq("tagname", QueryBuilder.bindMarker("tagname"))
         ).and(
            QueryBuilder.eq("tagvalue", QueryBuilder.bindMarker("tagvalue"))
         )
   )

   private val addReverseStaticTagsetStatement = session.prepare(
      QueryBuilder.update(keyspace, "reverse_tagset")
         .`with`(QueryBuilder.addAll("metrics", QueryBuilder.bindMarker("metric")))
         .where(
            QueryBuilder.eq("tagname", QueryBuilder.bindMarker("tagname"))
         ).and(
         QueryBuilder.eq("tagvalue", QueryBuilder.bindMarker("tagvalue"))
      )
   )

   private def removeReverseStaticTag(metric: String, tagName: String, tagValue: String) =
   {
      val boundedRemove = removeReverseStaticTagsetStatement
         .bind()
         .setList("metric", List(metric).asJava)
         .setString("tagname", tagName)
         .setString("tagvalue", tagValue)

      session.executeAsync(boundedRemove)
   }

   private def addReverseStaticTag(metric: String, tagName: String, tagValue: String) =
   {
      val boundedAdd = addReverseStaticTagsetStatement
         .bind()
         .setList("metric", List(metric).asJava)
         .setString("tagname", tagName)
         .setString("tagvalue", tagValue)

      session.executeAsync(boundedAdd)
   }

   /**
    * @param metric  A metric.
    * @return  The static tagset associated with the metric, if this metric exists in the database, None otherwise.
    */
   def getStaticTagset(metric: String): Option[Map[String, String]] =
   {
      val statement = QueryBuilder.select("tagset")
         .from(keyspace, "tagset")
         .where(QueryBuilder.eq("metric", metric))

      val result = Option(session.execute(statement).one())
      val prepared = result.map(r => r.getMap("tagset", classOf[String], classOf[String]).asScala.toMap)

      logger.debug(s"Static tagset retrieved for ${metric}: ${prepared}")

      prepared
   }

   def updateStaticTagset(metric: String, tags: Map[String, String]) = {
      val tagset = tags.toList

      def updateTagset() =
      {
         val statement = QueryBuilder.update(keyspace, "tagset")
            .`with`(QueryBuilder.putAll("tagset", tags.asJava))
            .where(QueryBuilder.eq("metric", metric))
            .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

         session.executeAsync(statement)
      }

      def updateReverseTagset(previousTagset: Map[String, String]) =
      {
         tags.foreach(tag => {
            addReverseStaticTag(metric, tag._1, tag._2)

            val valueHasChanged = previousTagset
               .get(tag._1)
               .map(previousValue => previousValue != tag._2)
               .getOrElse(false)

            if(valueHasChanged)
               removeReverseStaticTag(metric, tag._1, previousTagset(tag._1))
         })
      }

      if(!tagset.isEmpty)
      {
         val previousTagset = getStaticTagset(metric).getOrElse(Map.empty)

         updateTagset()
         updateReverseTagset(previousTagset)
      }
   }



   /**
    * Replaces an optional static tagset by a new one.
    *
    * @param metric  The metric associated with the tagset.
    * @param tagset  The new tagset associated with the metric.
    */
   def setStaticTagset(metric: String, tagset: Map[String, String]): Unit =
   {
      val previousTagset = getStaticTagset(metric).getOrElse(Map.empty)

      val statement = QueryBuilder.update(keyspace, "tagset")
         .`with`(QueryBuilder.set("tagset", tagset.asJava))
         .where(
            QueryBuilder.eq("metric", metric)
         )

      session.executeAsync(statement)

      tagset.foreach(tag => {
         if(previousTagset contains tag._1)
            removeReverseStaticTag(metric, tag._1, previousTagset(tag._1))
         addReverseStaticTag(metric, tag._1, tag._2)
      })
   }

   def getAllMetrics() =
   {
      val statement = QueryBuilder
         .select("metric")
         .distinct()
         .from(keyspace, "tagset")

      session.execute(statement).iterator().asScala.map(row => row.getString("metric")).toSet
   }
}
