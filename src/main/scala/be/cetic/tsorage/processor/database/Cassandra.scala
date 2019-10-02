package be.cetic.tsorage.processor.database

import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneOffset}
import java.util.Date

import be.cetic.tsorage.processor.datatype.DataTypeSupport
import be.cetic.tsorage.processor.sharder.{DaySharder, MonthSharder}
import be.cetic.tsorage.processor.update.{AggUpdate, RawUpdate}
import be.cetic.tsorage.processor.ProcessorConfig
import com.datastax.driver.core.querybuilder.QueryBuilder.insertInto
import com.datastax.driver.core.{Cluster, ConsistencyLevel, Session}
import com.typesafe.scalalogging.LazyLogging

object Cassandra extends LazyLogging {
  private val conf = ProcessorConfig

  private val rawKS = conf.rawKS
  private val aggKS = conf.aggKS

  private val cassandraHost = conf.conf.getString("cassandra.host")
  private val cassandraPort = conf.conf.getInt("cassandra.port")

  val session: Session = Cluster.builder
    .addContactPoint(cassandraHost)
    .withPort(cassandraPort)
    .withoutJMXReporting()
    .build
    .connect()


  val sharder = conf.conf.getString("sharder") match {
    case "day" => DaySharder
    case _ => MonthSharder
  }

  /**
    * Synchronously submits a raw value in the raw table.
    *
    * @param update The raw update to submit to Cassandra.
    */
  def submitRawUpdate[T](update: RawUpdate): Unit =
  {
    val ts = Timestamp.from(update.datetime.atOffset(ZoneOffset.UTC).toInstant)
    val support = DataTypeSupport.inferSupport(update)

    val baseStatement = insertInto(rawKS, "numeric")
       .value("metric_", update.metric)
       .value("shard_", sharder.shard(update.datetime))
       .value("datetime_", ts)
       .value(support.colname, support.asRawUdtValue(update.value))

    val statement = update.tagset
       .foldLeft(baseStatement)((st, tag) => st.value(tag._1, tag._2))
       .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

    logger.info(s"Cassandra submits raw value ${statement}")
    session.execute(statement.toString)
  }

  /**
    * Synchronously submits an aggregated value in the raw table.
    *
    * @param update The aggregated update to submit to Cassandra.
    */
  def submitAggUpdate[T](update: AggUpdate): Unit =
  {
    val ts = Timestamp.from(update.datetime.atOffset(ZoneOffset.UTC).toInstant)
    val support = DataTypeSupport.inferSupport(update)

    val baseStatement = insertInto(aggKS, "numeric")
       .value("metric_", update.metric)
       .value("shard_", Cassandra.sharder.shard(update.datetime))
       .value("interval_", update.interval)
       .value("aggregator_", update.aggregation)
       .value("datetime_", ts)
       .value(support.colname, support.asAggUdtValue(update.value))

    logger.debug(s"Base statement is ${baseStatement}")

    val statement = update.tagset
       .foldLeft(baseStatement)((st, tag) => st.value(tag._1, tag._2))
       .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

    logger.debug(s"Cassandra submits agg value ${statement}")

    session.execute(statement.toString)
  }
}
