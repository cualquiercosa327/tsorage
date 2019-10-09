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
            val boundedAdd = addReverseStaticTagsetStatement
               .bind()
               .setList("metric", List(metric).asJava)
               .setString("tagname", tag._1)
               .setString("tagvalue", tag._2)

            session.executeAsync(boundedAdd)

            val valueHasChanged = previousTagset
               .get(tag._1)
               .map(previousValue => previousValue != tag._2)
               .getOrElse(false)

            if(valueHasChanged)
            {
               val boundedRemove = removeReverseStaticTagsetStatement
                  .bind()
                  .setList("metric", List(metric).asJava)
                  .setString("tagname", tag._1)
                  .setString("tagvalue", previousTagset(tag._1))

               session.executeAsync(boundedRemove)
            }
         })
      }

      if(!tagset.isEmpty)
      {
         val previousTagset = getStaticTagset(metric).getOrElse(Map.empty)

         updateTagset()
         updateReverseTagset(previousTagset)
      }
   }
}
