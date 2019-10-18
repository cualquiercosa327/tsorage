package be.cetic.tsorage.common

import java.time.{LocalDateTime, ZoneOffset}
import be.cetic.tsorage.common.sharder.{MonthSharder, Sharder}
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

  private val session: Session = Cluster.builder
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
      .and(QueryBuilder.in("shard", QueryBuilder.bindMarker("shards")))
  )

  /**
   * @param metric A metric.
   * @return The static tagset associated with the metric.
   *         An empty map is returned if there is no static tagset associated with the metric.
   */
  def getStaticTagset(metric: String): Map[String, String] = {
    val statement = QueryBuilder.select("tagname", "tagvalue")
      .from(keyspaceAgg, "static_tagset")
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
   *
   * @param metric The metric to update.
   * @param tags   The static tags to update.
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
   * @param metric The metric associated with the tagset.
   * @param tagset The new tagset associated with the metric.
   */
  def setStaticTagset(metric: String, tagset: Map[String, String]): Unit = {
    val deleteStatement = QueryBuilder.delete()
      .from(keyspaceAgg, "static_tagset")
      .where(QueryBuilder.eq("metric", metric))
      .setConsistencyLevel(ConsistencyLevel.ONE)

    session.execute(deleteStatement)

    updateStaticTagset(metric, tagset)
  }

  /**
   * Retrieves the names of all the stored metrics.
   * For the moment, this list is limited to the metrics for which a static tagset has been defined, even if this tagset is empty. In the future, this limitation may be removed.
   *
   * @return The names of all the metrics having a defined static tagset.
   */
  def getAllMetrics() = {
    val statement = QueryBuilder
      .select("metric")
      .distinct()
      .from(keyspaceAgg, "static_tagset")
      .setConsistencyLevel(ConsistencyLevel.ONE)

    session.execute(statement).iterator().asScala.map(row => row.getString("metric"))
  }

  /**
   * @param tagname  The name of a static tag.
   * @param tagvalue The value of a static tag.
   * @return The names of all the metrics having the specified static tag.
   */
  def getMetricsWithStaticTag(tagname: String, tagvalue: String) = {
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
   * @param tagname  The name of the dynamic tag.
   * @param tagvalue The value of the dynamic tag.
   * @param from     The beginning of the time range to consider.
   * @param to       The end of the time range to consider.
   * @return The metrics having the given dynamic tag during the specified time range.
   *         The results are approximate, since dynamic tagsets are recorded by shard.
   */
  def getMetricsWithDynamigTag(tagname: String, tagvalue: String, from: LocalDateTime, to: LocalDateTime): Set[String] = {
    val shards = sharder.shards(from, to)

    val statement = QueryBuilder.select("metric")
      .from(keyspaceAgg, "reverse_dynamic_tagset")
      .where(QueryBuilder.eq("tagname", tagname))
      .and(QueryBuilder.eq("tagvalue", tagvalue))
      .and(QueryBuilder.in("shard", shards.asJava))
      .setConsistencyLevel(ConsistencyLevel.ONE)

    session.execute(statement)
      .asScala
      .map(row => row.getString("metric"))
      .toSet
  }

  /**
   * Retrieves the names of all the metrics having a given set of static tags.
   *
   * These names are collected by iteratively calculate the intersection of the metrics associated
   * with each of the tag set entries.
   *
   * @param tagset The set of static tags a metric must be associated with in order to be returned.
   *               If an empty tagset is provided, all known metrics are retrieved.
   * @return The metrics having the specified set of static tags.
   * @see getAllMetrics
   */
  def getMetricsWithStaticTagset(tagset: Map[String, String]) = tagset match {
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
   *
   * @param tagname The name of a static tag.
   * @return The values associated with tagname, as well as the metrics using the combined (tag name, tag value) as static tag.
   *         If the tag name is not in use, an empty set is retrieved.
   */
  def getStaticTagValues(tagname: String): Map[String, Set[String]] = {
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
   * @param metric A metric.
   * @param shards A set of shards.
   * @return All the dynamic tagsets associated with the metric during the specified shards.
   */
  def getDynamicTagset(metric: String, shards: Set[String]): Set[(String, String)] = {
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
   */
  def getDataFromTimeRange(metric: String, startDatetime: LocalDateTime, endDatetime: LocalDateTime): Seq[(LocalDateTime, BigDecimal)] = {
    // Compute shards.
    val sharder: Sharder = MonthSharder
    val shards = sharder.shards(startDatetime, endDatetime)

    // Convert datetimes to timestamps.
    val startTimestamp = startDatetime.toInstant(ZoneOffset.UTC).toEpochMilli
    val endTimestamp = endDatetime.toInstant(ZoneOffset.UTC).toEpochMilli
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

        val udtDouble = row.getUDTValue("value_double_")
        val udtLong = row.getUDTValue("value_long_")

        var valueOpt: Option[AnyVal] = None
        if (udtDouble != null) {
          valueOpt = Some(udtDouble.getDouble("value"))
        } else if (udtLong != null) {
          valueOpt = Some(udtLong.getLong("value"))
        }

        valueOpt match {
          case Some(value) =>
            // Convert the date to LocalDateTime and the value to BigDecimal.
            Some(LocalDateTime.ofInstant(date.toInstant, ZoneOffset.UTC) -> BigDecimal(value.toString))
          case None =>
            // This row is ignored because `value_double_` and `value_long_` are missing.
            None
        }
      }
    )

    data
  }
}

object Main {
  def main(args: Array[String]): Unit = {
    val startDatetime = LocalDateTime.of(2019, 1, 20, 17, 50, 0)
    val endDatetime = LocalDateTime.of(2019, 9, 21, 1, 10, 0)
    val database = new Cassandra(ConfigFactory.load("test.conf"))

    val data = database.getDataFromTimeRange("temperature", startDatetime, endDatetime)
    println("RESULT:")
    println(data)
  }
}
