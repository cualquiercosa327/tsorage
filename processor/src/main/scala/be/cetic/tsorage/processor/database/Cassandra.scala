package be.cetic.tsorage.processor.database

import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneOffset}
import java.util.{Collections, Date}

import be.cetic.tsorage.common.sharder.{DaySharder, MonthSharder, Sharder}
import be.cetic.tsorage.processor.datatype.DataTypeSupport
import be.cetic.tsorage.processor.update.{AggUpdate, RawUpdate}
import be.cetic.tsorage.processor.{Message, ProcessorConfig}
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.insertInto
import com.datastax.driver.core.{BatchStatement, Cluster, ConsistencyLevel, PreparedStatement, ResultSet, ResultSetFuture, Session}
import com.google.common.util.concurrent.{FutureCallback, Futures}
import com.typesafe.scalalogging.LazyLogging

import collection.JavaConverters._
import scala.concurrent.{ExecutionContextExecutor, Future, Promise}
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.util.Try


object Cassandra extends LazyLogging {
  private val conf = ProcessorConfig

  val rawKS = conf.rawKS
  val aggKS = conf.aggKS

  private val cassandraHost = conf.conf.getString("cassandra.host")
  private val cassandraPort = conf.conf.getInt("cassandra.port")

  /**
   * Converts a `ResultSetFuture` into a Scala `Future[ResultSet]`
   * @param f ResultSetFuture to convert
   * @return Converted Future
   */
  private def resultSetFutureToScala(f: ResultSetFuture): Future[ResultSet] = {
    val p = Promise[ResultSet]()
    Futures.addCallback(f,
      new FutureCallback[ResultSet] {
        def onSuccess(r: ResultSet) = p success r
        def onFailure(t: Throwable) = p failure t
      })
    p.future
  }

  // (tagnames, type) -> prepared statement
  private var rawStatementcache: Map[(Set[String], String), PreparedStatement] = Map()

  val session: Session = Cluster.builder
    .addContactPoint(cassandraHost)
    .withPort(cassandraPort)
    .withoutJMXReporting()
    .build
    .connect()

  val sharder = Sharder(conf.conf.getString("sharder"))

  private def preparedStatementRawInsert(msg: Message): PreparedStatement =
  {
    val key = (msg.tagset.keySet, msg.`type`)

    if (rawStatementcache contains key)
    {
      rawStatementcache(key)
    }
    else
    {
      val support = DataTypeSupport.inferSupport(msg.`type`)

      val baseStatement = QueryBuilder
         .insertInto(rawKS, "observations")
         .value("metric_", QueryBuilder.bindMarker("metric"))
         .value("shard_", QueryBuilder.bindMarker("shard"))
         .value("datetime_", QueryBuilder.bindMarker("datetime"))
         .value(support.colname, QueryBuilder.bindMarker("value"))

      val statement = msg.tagset.foldLeft(baseStatement)((base, tag) => base.value(tag._1, QueryBuilder.bindMarker(tag._1)))
      val prepared = session.prepare(statement)

      rawStatementcache = rawStatementcache ++ Map(key -> prepared)

      prepared
    }
  }

  /**
   * Asynchroneously submits the raw values of a message to Cassandra.
   * In order to keep a reasonable pressure to the Cassandra nodes,
   * the values are expected to belong to as few shards as possible, typically a single one.
   *
   * @param msg       The message to submit.
   * @param context
   * @return          The message itself, wrapped in a Future that will be achieved as soon as the values
   *                  are effectively submitted.
   *
   */
  def submitMessageAsync(msg: Message)(implicit context: ExecutionContextExecutor): Future[Message] =
  {
    val support = DataTypeSupport.inferSupport(msg.`type`)

    val prepared = preparedStatementRawInsert(msg)

    val statements = msg.values.map(record =>
      {
        val base = prepared.bind()
           .setString("metric", msg.metric)
           .setString("shard", sharder.shard(record._1))
           .setTimestamp("datetime", Timestamp.from(record._1.atOffset(ZoneOffset.UTC).toInstant))
           .setUDTValue("value", support.asRawUdtValue(record._2))

        msg.tagset.foldLeft(base)( (b, tag) => b.setString(tag._1, tag._2))
      }
    )

    val batch = new BatchStatement(BatchStatement.Type.UNLOGGED)
       .addAll(statements.asJava)
       .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

    resultSetFutureToScala(session.executeAsync(batch)).map(rs => msg)
  }

  def submitAggUpdateAsync(update: AggUpdate)(implicit context: ExecutionContextExecutor): Future[AggUpdate] =
  {
    val ts = Timestamp.from(update.datetime.atOffset(ZoneOffset.UTC).toInstant)
    val support = DataTypeSupport.inferSupport(update)

    val baseStatement = insertInto(aggKS, "observations")
       .value("metric_", update.metric)
       .value("shard_", Cassandra.sharder.shard(update.datetime))
       .value("interval_", update.interval)
       .value("aggregator_", update.aggregation)
       .value("datetime_", ts)
       .value(support.colname, support.asAggUdtValue(update.value))

    val statement = update.tagset
       .foldLeft(baseStatement)((st, tag) => st.value(tag._1, tag._2))
       .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

    //logger.debug(s"Cassandra submits agg value ${statement}")

    resultSetFutureToScala(session.executeAsync(statement)).map(rs => update)
  }
}
