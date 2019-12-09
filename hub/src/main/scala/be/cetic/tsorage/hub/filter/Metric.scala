package be.cetic.tsorage.hub.filter

import be.cetic.tsorage.common.TimeSeries
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.{ConsistencyLevel, Session}
import com.typesafe.config.Config

import scala.collection.JavaConverters._


/**
 * A representation of a metric.
 */
case class Metric(name: String, session: Session, conf: Config)
{
   private val keyspace = conf.getString("cassandra.keyspaces.other")

   /**
    * The static tagset associated with a metric.
    */
   private val staticTagsetStatement = session.prepare(
      QueryBuilder
         .select("tagname", "tagvalue")
         .from(keyspace, "static_tagset")
         .where(QueryBuilder.eq("metric", QueryBuilder.bindMarker("metric")))
   ).setConsistencyLevel(ConsistencyLevel.ONE)

   /**
    * The dynamic tagsets globally associated with the metric.
    */
   private val dynamicTagsetStatement = session.prepare(
      QueryBuilder
         .select("tagset")
         .from(keyspace, "dynamic_tagset")
         .where(QueryBuilder.eq("metric", QueryBuilder.bindMarker("metric")))
   ).setConsistencyLevel(ConsistencyLevel.ONE)

   /**
    * The dynamic tagsets globally associated with the metric.
    */
   private val shardedDynamicTagsetStatement = session.prepare(
      QueryBuilder
         .select("tagset")
         .from(keyspace, "sharded_dynamic_tagset")
         .where(QueryBuilder.eq("metric", QueryBuilder.bindMarker("metric")))
         .and(QueryBuilder.eq("shard", QueryBuilder.bindMarker("shard")))
   ).setConsistencyLevel(ConsistencyLevel.ONE)


   // =====================

   /**
    * @return The static tagset associated with this metric.
    */
   def getStaticTagset(): Map[String, String] = {
      val res = session.execute(
         staticTagsetStatement
            .bind()
            .setString("metric", name)
      )

      res.asScala.map(row => (row.getString("tagname") -> row.getString("tagvalue"))).toMap
   }

   /**
    * @return All the time series associated with this metric.
    */
   def getTimeSeries(): Set[TimeSeries] =
   {
      session.execute(
         dynamicTagsetStatement
            .bind()
            .setString("metric", this.name)
      ).asScala
         .map(row => TimeSeries(
            name,
            row.getMap("tagset", classOf[String], classOf[String]).asScala.toMap
         )
      ).toSet
   }

   /**
    * @param shard A shard.
    * @return  All the time series associated with this metric in the specified shard.
    */
   def getTimeSeries(shard: String): Set[TimeSeries] =
   {
      session.execute(
         shardedDynamicTagsetStatement
            .bind()
            .setString("metric", this.name)
            .setString("shard", shard)
      ).asScala
         .map(row => TimeSeries(
            name,
            row.getMap("tagset", classOf[String], classOf[String]).asScala.toMap
         )
         ).toSet
   }
}