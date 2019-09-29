package be.cetic.tsorage.processor.flow

import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneId, ZoneOffset}

import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.sharder.Sharder
import be.cetic.tsorage.processor.{Message, Observation, ProcessorConfig}
import com.datastax.driver.core.querybuilder.Insert
import com.datastax.driver.core.querybuilder.QueryBuilder.{bindMarker, insertInto}
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.datastax.oss.driver.shaded.guava.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContextExecutor
import scala.util.Try

class CassandraFlow(sharder: Sharder)(implicit val ec: ExecutionContextExecutor) {

  private val config = ProcessorConfig.conf

  private val rawKeyspace = config.getString("cassandra.keyspaces.raw")
  private val aggKeyspace = config.getString("cassandra.keyspaces.aggregated")

  private implicit val session = Cassandra.session

  def bindRawInsert[T]: (Observation[T], PreparedStatement) => BoundStatement = (observation: Observation[T], prepared: PreparedStatement) => {
    val ts = Timestamp.from(observation.datetime.atOffset(ZoneOffset.UTC).toInstant)

    val baseBound = prepared.bind()
      .setString("metric_", observation.metric)
      .setString("shard_", sharder.shard(observation.datetime))
      .setTimestamp("datetime_", ts)
      .set(observation.support.colname, observation.value, observation.support.codec)

    val folder: (BoundStatement, (String, String)) => BoundStatement = (prev: BoundStatement, tag: (String, String)) => prev.setString(tag._1, tag._2)

    observation.tagset.foldLeft(baseBound)(folder)
  }

  def getRawInsertPreparedStatement[T]: Observation[T] => PreparedStatement = { obs => {
    val cache: LoadingCache[Set[String], PreparedStatement] = CacheBuilder.newBuilder()
       .maximumSize(100)
       .build(
         new CacheLoader[Set[String], PreparedStatement] {
           def load(tags: Set[String]): PreparedStatement = {
             val tagnames = tags.toList
             val tagMarkers = tags.map(tag => bindMarker(tag)).toList

             val baseStatement = insertInto(rawKeyspace, "numeric")
                .value("metric_", bindMarker("metric_"))
                .value("shard_", bindMarker("shard_"))
                .value("datetime_", bindMarker("datetime_"))
                .value(obs.support.colname, bindMarker(obs.support.colname))

             val folder: (Insert, String) => Insert = (base, tagname) => base.value(tagname, bindMarker(tagname))
             val finalStatement = tags.foldLeft(baseStatement)(folder)

             session.prepare(finalStatement)
           }
         }
       )

    val f: Observation[T] => PreparedStatement = observation => {
      cache.get(observation.tagset.keySet)
    }

    f(obs)
  }


  }

  /**
    * A function ensuring all tagnames contained in a message
    * are prepared in the Cassandra database. The retrieved object
    * is the message itself, and the tagname management is a side effect.
    */
  def notifyTagnames: Message => Message = {

    var cache: Set[String] = Set()

    val f: Message => Message = msg => {
      val recentTags = msg.tagset.keySet.diff(cache)
      cache = cache ++ recentTags

      recentTags.map(tag => s"""ALTER TABLE ${rawKeyspace}.numeric ADD "${tag.replace("\"", "\"\"")}" text;""")
        .foreach(t => Try(session.execute(t)))

      recentTags.map(tag => s"""ALTER TABLE ${aggKeyspace}.numeric ADD "${tag.replace("\"", "\"\"")}" text;""")
        .foreach(t => Try(session.execute(t)))

      msg
    }

    f
  }

  /**
    * Extracts a datetime from an observation.
    *
  def observationToTime[T]: Observation[T] => (String, String, LocalDateTime) = observation =>
    (observation.metric, sharder.shard(observation.datetime), observation.datetime)

  def rawFlow[T] = AltCassandraFlow.createWithPassThrough[Observation[T]](16,
    getRawInsertPreparedStatement,
    bindRawInsert)
    */
}
