package be.cetic.tsorage.processor.database

import java.sql.Timestamp
import java.time.ZoneOffset

import be.cetic.tsorage.common.CassandraFactory
import be.cetic.tsorage.common.messaging.{AggUpdate, Message, Observation}
import be.cetic.tsorage.common.sharder.Sharder
import be.cetic.tsorage.processor.datatype.{DataTypeSupport, MetaSupportInfer}
import be.cetic.tsorage.processor.ProcessorConfig
import com.datastax.driver.core.{BatchStatement, Cluster, ConsistencyLevel, PreparedStatement, ResultSet, ResultSetFuture, Session}
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.google.common.util.concurrent.{FutureCallback, Futures}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContextExecutor, Future, Promise}
import scala.language.{implicitConversions, postfixOps}


object Cassandra extends LazyLogging {
  private val conf = ProcessorConfig

  val rawKS = conf.rawKS
  val aggKS = conf.aggKS

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
  private var rawStatementcache: Map[String, PreparedStatement] = Map()
  private var aggStatementCache: Map[String, PreparedStatement] = Map()

  val session: Session = CassandraFactory.createCassandraSession(conf.conf)

  val sharder = Sharder(conf.conf.getString("sharder"))

  private def preparedStatementObservationInsert(support: DataTypeSupport[_]) =
  {
    if (rawStatementcache contains support.`type`)
    {
      rawStatementcache(support.`type`)
    }
    else
    {
      val statement = QueryBuilder
         .insertInto(rawKS, "observations")
         .value("metric", QueryBuilder.bindMarker("metric"))
         .value("tagset", QueryBuilder.bindMarker("tagset"))
         .value("shard", QueryBuilder.bindMarker("shard"))
         .value("datetime", QueryBuilder.bindMarker("datetime"))
         .value(support.colname, QueryBuilder.bindMarker("value"))

      val prepared = session.prepare(statement)

      rawStatementcache = rawStatementcache ++ Map(support.`type` -> prepared)

      prepared
    }
  }

  private def preparedStatementRawInsert(msg: Message): PreparedStatement =
  {
    val support = DataTypeSupport.inferSupport(msg.`type`)
    preparedStatementObservationInsert(support)
  }

  private def preparedStatementAggInsert(update: AggUpdate): PreparedStatement =
  {
    val key = update.`type`

    if (aggStatementCache contains key)
    {
      aggStatementCache(key)
    }
    else
    {
      val support = MetaSupportInfer.inferSupport(update.`type`)

      val statement = QueryBuilder
         .insertInto(aggKS, "observations")
         .value("metric", QueryBuilder.bindMarker("metric"))
         .value("tagset", QueryBuilder.bindMarker("tagset"))
         .value("shard", QueryBuilder.bindMarker("shard"))
         .value("datetime", QueryBuilder.bindMarker("datetime"))
         .value("interval", QueryBuilder.bindMarker("interval"))
         .value("aggregator", QueryBuilder.bindMarker("aggregator"))
         .value(support.colname, QueryBuilder.bindMarker("value"))

      val prepared = session.prepare(statement)
                            .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

      aggStatementCache = aggStatementCache ++ Map(key -> prepared)

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
    logger.debug(s"Submit message ${msg}")

    val support = DataTypeSupport.inferSupport(msg.`type`)

    val prepared = preparedStatementRawInsert(msg)

    val statements = msg.values.map(record =>
      {
        prepared.bind()
           .setString("metric", msg.metric)
           .setMap("tagset", msg.tagset.asJava)
           .setString("shard", sharder.shard(record._1))
           .setTimestamp("datetime", Timestamp.from(record._1.atOffset(ZoneOffset.UTC).toInstant))
           .setUDTValue("value", support.asRawUdtValue(record._2))
      }
    )

    val batch = new BatchStatement(BatchStatement.Type.UNLOGGED)
       .addAll(statements.asJava)
       .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

    resultSetFutureToScala(session.executeAsync(batch)).map(rs => msg)
  }

  def submitAggUpdateAsync(update: AggUpdate)(implicit context: ExecutionContextExecutor): Future[AggUpdate] =
  {
    val support = MetaSupportInfer.inferSupport(update.`type`)

    val bound = preparedStatementAggInsert(update).bind()
       .setString("metric", update.ts.metric)
       .setMap("tagset", update.ts.tagset.asJava)
       .setString("shard", sharder.shard(update.datetime))
       .setString("interval", update.interval)
       .setString("aggregator", update.aggregation)
       .setTimestamp("datetime", Timestamp.from(update.datetime.atOffset(ZoneOffset.UTC).toInstant))
       .setUDTValue("value", support.asAggUdtValue(update.value))

    resultSetFutureToScala(session.executeAsync(bound)).map(rs => update)
  }

  /**
   * Inserts a raw observation to Cassandra.
   * @param obs   The observation to insert.
   * @param context
   * @return a Future of obs, that becomes reality once the observation has been inserted.
   */
  def submitObservationAsync(obs: Observation)(implicit context: ExecutionContextExecutor): Future[Observation] =
  {
    val support = DataTypeSupport.inferSupport(obs.`type`)

    val bound = preparedStatementObservationInsert(support).bind()
       .setString("metric", obs.metric)
       .setMap("tagset", obs.tagset.asJava)
       .setString("shard", sharder.shard(obs.datetime))
       .setTimestamp("datetime", Timestamp.from(obs.datetime.atOffset(ZoneOffset.UTC).toInstant))
       .setUDTValue("value",support.asRawUdtValue(obs.value))

    resultSetFutureToScala(session.executeAsync(bound)).map(rs => obs)
  }
}
