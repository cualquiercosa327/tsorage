package be.cetic.tsorage.processor.database

import java.time.LocalDateTime
import java.util.Date

import be.cetic.tsorage.processor.DAO.{datetimeFormatter, logger}
import be.cetic.tsorage.processor.sharder.{DaySharder, MonthSharder}
import com.datastax.driver.core.querybuilder.QueryBuilder.insertInto
import com.datastax.driver.core.{Cluster, ConsistencyLevel, Session}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

object Cassandra extends LazyLogging {
  private val conf = ConfigFactory.load("storage.conf")
  private val cassandraHost = conf.getString("cassandra.host")
  private val cassandraPort = conf.getInt("cassandra.port")

  val session: Session = Cluster.builder
    .addContactPoint(cassandraHost)
    .withPort(cassandraPort)
    .withoutJMXReporting()
    .build
    .connect()

  val sharder = conf.getString("sharder") match {
    case "day" => DaySharder
    case _ => MonthSharder
  }

  /**
    * Synchronously submits a raw value in the rax table.
    * @param metric
    * @param shard
    * @param tagset
    * @param datetime
    * @param value
    */
  def submitValue(
                    metric: String,
                    shard: String,
                    tagset: Map[String, String],
                    datetime: LocalDateTime,
                    value: Float): Unit =
  {
    val baseStatement = insertInto("tsorage_raw", "numeric")
       .value("metric", metric)
       .value("shard", shard)
       .value("datetime", datetime.format(datetimeFormatter))
       .value("value", value)

    val statement = tagset
       .foldLeft(baseStatement)((st, tag) => st.value(tag._1, tag._2))
       .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

    logger.info(s"DAO submits value ${statement}")
    session.execute(statement.toString)
  }

  /**
    * Synchronously submits a value in the aggregated table.
    * @param metric
    * @param shard
    * @param period
    * @param aggregator
    * @param tagset
    * @param datetime
    * @param value
    */
  def submitValue(
                    metric: String,
                    shard: String,
                    period: String,
                    aggregator: String,
                    tagset: Map[String, String],
                    datetime: LocalDateTime,
                    value: Float): Unit =
  {
    val baseStatement = insertInto("tsorage_agg", "numeric")
       .value("metric", metric)
       .value("shard", shard)
       .value("interval", period)
       .value("aggregator", aggregator)
       .value("datetime", datetime.format(datetimeFormatter))
       .value("value", value)

    val statement = tagset
       .foldLeft(baseStatement)((st, tag) => st.value(tag._1, tag._2))
       .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

    logger.info(s"DAO submits value ${statement}")

    session.execute(statement.toString)
  }

  /**
    * Synchronously submits a value in the aggregated table.
    * @param metric
    * @param shard
    * @param period
    * @param aggregator
    * @param tagset
    * @param datetime
    * @param value
    */
  def submitTemporalValue(
                            metric: String,
                            shard: String,
                            period: String,
                            aggregator: String,
                            tagset: Map[String, String],
                            datetime: LocalDateTime,
                            observationDatetime: Date,
                            value: Float): Unit =
  {
    val baseStatement = insertInto("tsorage_agg", "numeric")
       .value("metric", metric)
       .value("shard", shard)
       .value("interval", period)
       .value("aggregator", aggregator)
       .value("datetime", datetime.format(datetimeFormatter))
       .value("observation_datetime", observationDatetime)
       .value("value", value)

    val statement = tagset
       .foldLeft(baseStatement)((st, tag) => st.value(tag._1, tag._2))
       .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

    logger.info(s"DAO submits value ${statement}")

    session.execute(statement.toString)
  }


}
