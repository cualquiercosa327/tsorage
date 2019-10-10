package be.cetic.tsorage.hub

import com.datastax.driver.core.querybuilder.QueryBuilder.insertInto
import com.datastax.driver.core.{Cluster, ConsistencyLevel, Row, Session}
import com.datastax.oss.driver.api.querybuilder.select.SelectFrom
import com.typesafe.config.ConfigFactory
import com.datastax.oss.driver.api.querybuilder.QueryBuilder._
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
    * @return  The static tagset associated with the metric, if this metric exists in the database, None otherwise.
    */
   def getStaticTagset(metric: String): Option[Map[String, String]] =
   {
      val statement = selectFrom(keyspace, "tagset")
         .column("tagset")
         .whereColumn("metric").isEqualTo(literal(metric))
         .build()

      val result = Option(session.execute(statement.getQuery).one())
      val prepared = result.map(r => r.getMap("tagset", classOf[String], classOf[String]).asScala.toMap)

      logger.debug(s"Static tagset retrieved for ${metric}: ${prepared}")

      prepared
   }
}
