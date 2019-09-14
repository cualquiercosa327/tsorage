package be.cetic.tsorage.processor.flow

import java.time.LocalDateTime

import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.sharder.Sharder
import be.cetic.tsorage.processor.{FloatMessage, FloatObservation}
import com.datastax.driver.core.querybuilder.Insert
import com.datastax.driver.core.querybuilder.QueryBuilder.{bindMarker, insertInto}
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.datastax.oss.driver.shaded.guava.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContextExecutor
import scala.util.Try

class CassandraFlow(sharder: Sharder)(implicit val ec: ExecutionContextExecutor) {
  private val config = ConfigFactory.load("tsorage.conf")
  private val keyspaceRaw = config.getString("cassandra.keyspaces.raw")
  private implicit val session = Cassandra.session

  val bindRawInsert: (FloatObservation, PreparedStatement) => BoundStatement = (observation: FloatObservation, prepared: PreparedStatement) => {

    val baseBound = prepared.bind()
      .setString("metric_", observation.metric)
      .setString("shard_", sharder.shard(observation.datetime))
      .setTimestamp("datetime_", java.sql.Timestamp.valueOf(observation.datetime))
      .setFloat("value_", observation.value)

    val folder: (BoundStatement, (String, String)) => BoundStatement = (prev: BoundStatement, tag: (String, String)) => prev.setString(tag._1, tag._2)

    observation.tagset.foldLeft(baseBound)(folder)
  }

  val getRawInsertPreparedStatement: FloatObservation => PreparedStatement = {

    val cache: LoadingCache[Set[String], PreparedStatement] = CacheBuilder.newBuilder()
      .maximumSize(100)
      .build(
        new CacheLoader[Set[String], PreparedStatement] {
          def load(tags: Set[String]): PreparedStatement = {
            val tagnames = tags.toList
            val tagMarkers = tags.map(tag => bindMarker(tag)).toList

            val baseStatement = insertInto(keyspaceRaw, "numeric")
              .value("metric_", bindMarker("metric_"))
              .value("shard_", bindMarker("shard_"))
              .value("datetime_", bindMarker("datetime_"))
              .value("value_", bindMarker("value_"))

            val folder: (Insert, String) => Insert = (base, tagname) => base.value(tagname, bindMarker(tagname))
            val finalStatement = tags.foldLeft(baseStatement)(folder)

            session.prepare(finalStatement)
          }
        }
      )

    val f: FloatObservation => PreparedStatement = observation => {
      cache.get(observation.tagset.keySet)
    }

    f
  }

  /**
    * A function ensuring all tagnames contained in a message
    * are prepared in the Cassandra database. The retrieved object
    * is the message itself, and the tagname management is a side effect.
    */
  val notifyTagnames: FloatMessage => FloatMessage = {

    var cache: Set[String] = Set()

    val f: FloatMessage => FloatMessage = msg => {
      val recentTags = msg.tagset.keySet.diff(cache)
      cache = cache ++ recentTags

      recentTags.map(tag => s"""ALTER TABLE tsorage_raw.numeric ADD "${tag.replace("\"", "\"\"")}" text;""")
        .foreach(t => Try(session.execute(t)))

      recentTags.map(tag => s"""ALTER TABLE tsorage_agg.numeric ADD "${tag.replace("\"", "\"\"")}" text;""")
        .foreach(t => Try(session.execute(t)))

      msg
    }

    f
  }

  /**
    * Extracts a datetime from an observation.
    */
  val observationToTime: FloatObservation => (String, String, LocalDateTime) = observation =>
    (observation.metric, sharder.shard(observation.datetime), observation.datetime)

  val rawFlow = AltCassandraFlow.createWithPassThrough[FloatObservation](16,
    getRawInsertPreparedStatement,
    bindRawInsert)
}
