package be.cetic.tsorage.common

import java.time.LocalDateTime

import be.cetic.tsorage.common.sharder.Sharder
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.select
import com.datastax.driver.core.{Cluster, ConsistencyLevel, Session}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

import collection.JavaConverters._

/**
 * An access to the Cassandra cluster
 */
class Cassandra(private val conf: Config = ConfigFactory.load("common.conf")) extends LazyLogging {
  private val cassandraHost = conf.getString("cassandra.host")
  private val cassandraPort = conf.getInt("cassandra.port")

  private val keyspaceAgg = conf.getString("cassandra.keyspaces.other") // Keyspace containing aggregated data.
  private val keyspaceRaw = conf.getString("cassandra.keyspaces.raw") // Keyspace containing raw data.

  val session: Session = Cluster.builder
      .addContactPoint(cassandraHost)
      .withPort(cassandraPort)
      .withoutJMXReporting()
      .build
      .connect()

  val sharder = Sharder(conf.getString("sharder"))

  private val getDynamicTagsetStatement = session.prepare(
      QueryBuilder.select("tagname", "tagvalue")
         .from(keyspaceAgg, "dynamic_tagset")
         .where(QueryBuilder.eq("metric", QueryBuilder.bindMarker("metric")))
   )

  /**
    * Updates a subset of the static tags associated with a metric.
    * @param metric  The metric to update.
    * @param tags    The static tags to update.
    */
   def updateStaticTagset(metric: String, tags: Map[String, String]) = {
      tags.foreach(tag => {
         val statement = QueryBuilder.update(keyspaceAgg, "static_tagset")
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
         .from(keyspaceAgg, "static_tagset")
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
         .from(keyspaceAgg, "reverse_static_tagset")
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
         .from(keyspaceAgg, "reverse_static_tagset")
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

  /**
   * Get data from a time range for a single metric.
   *
   * @param metric        A metric.
   * @param startDatetime A start time.
   * @param endDatetime   An end time.
   * @return data within the given time interval ordered according to ascending datetime.
   */
  def getDataFromTimeRange(metric: String, startDatetime: LocalDateTime,
                           endDatetime: LocalDateTime): Seq[(LocalDateTime, BigDecimal)] = {
    // Compute shards.
    var shards = sharder.shards(startDatetime, endDatetime)

    // Reverse the order of shards (ascending to descending).
    // Explanation of why we do this:
    //   Problem: our goal is to return data by ascending datetime. However, in the database, shards are ordered in
    //     ascending order and data (within a column) according to descending datetime.
    //   Solution: we reverse the order of shards and the order of data retrieved in the database (see below the
    //     return line). Reversing the order of shards ensures that `data` (see below) is ordered according to
    //     descending datetime. As a result, we can just reverse the order of `data` to obtain a sequence of data
    //     ordered by ascending datetime.
    shards = shards.reverse

    // Convert datetimes to timestamps.
    val startTimestamp = DateTimeConverter.localDateTimeToEpochMilli(startDatetime)
    val endTimestamp = DateTimeConverter.localDateTimeToEpochMilli(endDatetime)
    // Query the database.
    val results = for (shard <- shards)
      yield {
        val statement = QueryBuilder.select("datetime_", "value_double_", "value_long_")
          .from(keyspaceRaw, "observations")
          .where(QueryBuilder.eq("metric_", metric))
          .and(QueryBuilder.eq("shard_", shard))
          .and(QueryBuilder.gte("datetime_", startTimestamp))
          .and(QueryBuilder.lte("datetime_", endTimestamp))

        session.executeAsync(statement)
      }

    val data = results.flatMap(
      _.getUninterruptibly().all().asScala.flatMap { row =>
        val date = row.getTimestamp("datetime_")

        val udtDouble = Option(row.getUDTValue("value_double_"))
        val udtLong = Option(row.getUDTValue("value_long_"))

        // Take either "value_double_" or "value_long_" or neither (depending on whether they are None or not).
        var valueOpt: Option[AnyVal] = None
        if (udtDouble.isDefined) {
          valueOpt = Some(udtDouble.get.getDouble("value"))
        } else if (udtLong.isDefined) {
          valueOpt = Some(udtLong.get.getLong("value"))
        }

        valueOpt match {
          case Some(value) =>
            // Convert the date to LocalDateTime and the value to BigDecimal.
            Some(DateTimeConverter.dateToLocalDateTime(date) -> BigDecimal(value.toString))
          case None =>
            // This row is ignored because `value_double_` and `value_long_` are missing.
            None
        }
      }
    )

    data.reverse // As `data` is ordered according to descending datetime, it is necessary to reverse this sequence
    // (we want data to be ordered by ascending datetime).
  }
}
