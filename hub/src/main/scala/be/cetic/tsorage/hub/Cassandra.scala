package be.cetic.tsorage.hub

import java.time.LocalDateTime

import be.cetic.tsorage.common.{DateTimeConverter, SingleData, TimeSeries}
import be.cetic.tsorage.common.sharder.Sharder
import be.cetic.tsorage.hub.filter.Metric
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.select
import com.datastax.driver.core.{Cluster, ConsistencyLevel, Session}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

/**
 * An access to the Cassandra cluster
 */
class Cassandra(private val conf: Config) extends LazyLogging {
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
   * Get data from a time range of a time series (that is, a metric with a specific tagset).
   *
   * @param timeSeries    A time series.
   * @param startDatetime A start time.
   * @param endDatetime   An end time.
   * @return data of the tagset within the given time interval ordered according to ascending datetime.
   */
  def getDataFromTimeRange(timeSeries: TimeSeries, startDatetime: LocalDateTime,
                           endDatetime: LocalDateTime): Seq[SingleData] = {

    val metric = timeSeries.metric
    val tagset = timeSeries.tagset

    // Compute all shards into the time range.
    val shards = sharder.shards(startDatetime, endDatetime)

    // Convert datetimes to timestamps.
    val startTimestamp = DateTimeConverter.localDateTimeToEpochMilli(startDatetime)
    val endTimestamp = DateTimeConverter.localDateTimeToEpochMilli(endDatetime)

    // Query the database.
    val results = for (shard <- shards)
      yield {
        val statement = QueryBuilder.select("datetime", "value_double", "value_long")
          .from(keyspaceRaw, "observations")
          .where(QueryBuilder.eq("metric", metric))
          .and(QueryBuilder.eq("shard", shard))
          .and(QueryBuilder.eq("tagset", tagset.asJava))
          .and(QueryBuilder.gte("datetime", startTimestamp))
          .and(QueryBuilder.lte("datetime", endTimestamp))

        session.executeAsync(statement)
      }

    val data = results.flatMap(
      _.getUninterruptibly().all().asScala.flatMap { row =>
        val date = row.getTimestamp("datetime")

        val udtDouble = Option(row.getUDTValue("value_double"))
        val udtLong = Option(row.getUDTValue("value_long"))

        // Take either "value_double_" or "value_long_" or neither (depending on whether they are None or not).
        var valueOpt: Option[AnyVal] = None
        if (udtDouble.isDefined) {
          valueOpt = Some(udtDouble.get.getDouble("value"))
        } else if (udtLong.isDefined) {
          valueOpt = Some(udtLong.get.getLong("value"))
        }

        valueOpt match {
          case Some(value) =>
            // Create a single data.
            Some(SingleData(
              timeSeries,
              DateTimeConverter.dateToLocalDateTime(date), // Convert the date to LocalDateTime and the value to BigDecimal.
              BigDecimal(value.toString)
            ))
          case None =>
            // This row is ignored because `value_double_` and `value_long_` are missing.
            None
        }
      }.reverse // Data of a shard are ordered according to descending datetime. Therefore, we have to reverse them to
      // obtain a sequence of data ordered by ascending datetime.
    )

    data
  }


  /**
   * Get data from a time range of a single metric independently of its tagsets (data of all its time series / tagsets).
   *
   * @param metric        A metric.
   * @param startDatetime A start time.
   * @param endDatetime   An end time.
   * @return data of the metric within the given time interval ordered according to ascending datetime.
   */
  def getDataFromTimeRange(metric: String, startDatetime: LocalDateTime,
                           endDatetime: LocalDateTime): Seq[SingleData] = {

    /**
     * Compare two data. Return true if the datetime of `data1` is earlier than the datetime of `data2`.
     *
     * @param data1 A single data.
     * @param data2 A single data
     * @return Return true if the datetime of `data1` is earlier than the datetime of `data2`, false otherwise.
     */
    def compareData(data1: SingleData, data2: SingleData): Boolean = {
      data1.datetime.compareTo(data2.datetime) <= 0
    }

    /**
     * Merge two sorted lists into one sorted list.
     *
     * @param list1 A first sorted list.
     * @param list2 A second sorted list.
     * @param compare Compare two items. If the first one is less than the second one, then this function returns true,
     *                otherwise, it returns false.
     * @tparam A Some item (Int, Double, Object, etc.).
     * @return The merger of the two lists.
     */
    def mergeLists[A](list1: List[A], list2: List[A], compare: (A, A) => Boolean): List[A] = {
      (list1, list2) match {
        case (Nil, Nil) => Nil // `list1` and `list2` are empty.
        case (head1::tail1, Nil) => list1 // `list1` contains at least one item and `list2` is empty.
        case (Nil, head2::tail2) => list2 // `list2` contains at least one item and `list1` is empty.
        case (head1::tail1, head2::tail2) => { // // `list1` and `list2` contain at least one item.
          if (compare(head1, head2)) {
            head1 :: mergeLists(tail1, list2, compare)
          } else {
            head2 :: mergeLists(list1, tail2, compare)
          }
        }
      }
    }

    /**
     * Merge a sequence of sorted lists into one sorted list.
     *
     * @param listSeq A sequence of sorted lists.
     * @param compare Compare two items. If the first one is less than the second one, then this function returns true,
     *                otherwise, it returns false.
     * @tparam A Some item (Int, Double, Any, List, etc.).
     * @return The merger of all lists.
     */
    @scala.annotation.tailrec
    def mergeListSeq[A](listSeq: Seq[List[A]], compare: (A, A) => Boolean): Seq[A] = {
      // Group lists two by two and sort them.
      val sortedListSeq = listSeq.grouped(2).map { listPair => // `grouped(2)` tries to create pairs.
        if (listPair.size == 1) { // If `listPair` is not a pair.
          listPair.head // Yield the only list contained in `listPair`.
        } else { // If `listPair` is a pair.
          mergeLists(listPair.head, listPair(1), compare) // Merge the two lists.
        }
      }.toSeq

      if (sortedListSeq.size == 1) { // All lists have been merged.
        sortedListSeq.head
      } else { // All lists have not yet been merged.
        mergeListSeq(sortedListSeq, compare)
      }
    }

    // Get all time series of this metric.
    val timeSeriesList = Metric(metric, session, conf).getTimeSeries() + TimeSeries(metric, Map())

    // Get data of this metric asynchronously.
    val dataFuture = Future.sequence(timeSeriesList.map(timeSeries => {
      Future {
        // Get data of this time series.
        getDataFromTimeRange(timeSeries, startDatetime, endDatetime).toList
      }
    }))

    // Wait the data.
    val dataList = Await.result(dataFuture, Duration.Inf).toList

    mergeListSeq(dataList, compareData)
  }
}
