package be.cetic.tsorage.processor.database

import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneOffset}
import java.util.Date

import be.cetic.tsorage.processor.ProcessorConfig
import be.cetic.tsorage.processor.sharder.{DaySharder, MonthSharder}
import com.datastax.driver.core.querybuilder.QueryBuilder.insertInto
import com.datastax.driver.core.{Cluster, ConsistencyLevel, Session}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

object Cassandra extends LazyLogging {
  private val conf = ProcessorConfig.conf

  private val rawKeyspace = conf.getString("cassandra.keyspaces.raw")
  private val aggKeyspace = conf.getString("cassandra.keyspaces.aggregated")

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
    * Synchronously submits a raw value in the raw table.
    *
    * @param metric
    * @param shard
    * @param tagset
    * @param datetime
    * @param value
    */
  def submitValue[T](
                    metric: String,
                    shard: String,
                    tagset: Map[String, String],
                    datetime: LocalDateTime,
                    valueColumn: String,
                    value: T): Unit =
  {
    val ts = Timestamp.from(datetime.atOffset(ZoneOffset.UTC).toInstant)

    val baseStatement = insertInto(rawKeyspace, "numeric")
       .value("metric_", metric)
       .value("shard_", shard)
       .value("datetime_", ts)
       .value(valueColumn, value)

    val statement = tagset
       .foldLeft(baseStatement)((st, tag) => st.value(tag._1, tag._2))
       .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

    logger.info(s"Cassandra submits raw value ${statement}")
    session.execute(statement.toString)
  }

  /**
    * Synchronously submits a value in the aggregated table.
    *
    * @param metric
    * @param shard
    * @param period
    * @param aggregator
    * @param tagset
    * @param datetime
    * @param value
    */
  def submitValue[T](
                    metric: String,
                    shard: String,
                    period: String,
                    aggregator: String,
                    tagset: Map[String, String],
                    datetime: LocalDateTime,
                    valueColumn: String,
                    value: T): Unit =
  {
    val ts = Timestamp.from(datetime.atOffset(ZoneOffset.UTC).toInstant)


    val baseStatement = insertInto(aggKeyspace, "numeric")
       .value("metric_", metric)
       .value("shard_", shard)
       .value("interval_", period)
       .value("aggregator_", aggregator)
       .value("datetime_", ts)
       .value(valueColumn, value)
    logger.info(s"Base statement is ${baseStatement}")

    val statement = tagset
       .foldLeft(baseStatement)((st, tag) => st.value(tag._1, tag._2))
       .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

    logger.info(s"Cassandra submits value ${statement}")

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
  def submitTemporalValue[T](
                            metric: String,
                            shard: String,
                            period: String,
                            aggregator: String,
                            tagset: Map[String, String],
                            datetime: LocalDateTime,
                            observationDatetime: Date,
                            valueColumn: String,
                            value: T): Unit =
  {
    val ts = Timestamp.from(datetime.atOffset(ZoneOffset.UTC).toInstant)

    val baseStatement = insertInto(aggKeyspace, "numeric")
       .value("metric_", metric)
       .value("shard_", shard)
       .value("interval_", period)
       .value("aggregator_", aggregator)
       .value("datetime_", ts)
       .value("observation_datetime_", observationDatetime)
       .value(valueColumn, value)

    val statement = tagset
       .foldLeft(baseStatement)((st, tag) => st.value(tag._1, tag._2))
       .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

    logger.info(s"Cassandra submits value ${statement}")

    session.execute(statement.toString)
  }
}
