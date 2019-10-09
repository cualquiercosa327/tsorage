package be.cetic.tsorage.hub

import com.datastax.driver.core.querybuilder.QueryBuilder
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
         val tagsetStatement = QueryBuilder.update(keyspace, "tagset")
            .`with`(QueryBuilder.putAll("tagset", tags.asJava))
            .where(QueryBuilder.eq("metric", metric))
            .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

         session.executeAsync(tagsetStatement)
      }

      def updateReverseTagset() = {
         // TODO
      }

      if(!tagset.isEmpty)
      {
         updateTagset()
         updateReverseTagset()
      }
   }
}
