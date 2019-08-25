package be.cetic.tsorage.processor.flow

import akka.stream.alpakka.cassandra.scaladsl.AltCassandraFlow
import be.cetic.tsorage.processor.{FloatMessage, FloatObservation}
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.sharder.Sharder
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Session}
import com.datastax.driver.core.querybuilder.Insert
import com.datastax.driver.core.querybuilder.QueryBuilder.{bindMarker, insertInto}
import com.datastax.oss.driver.shaded.guava.common.cache.{CacheBuilder, CacheLoader}

import scala.util.Try

class CassandraFlow(sharder: Sharder) {
  val session = Cassandra.session
  val bindRawInsert: (FloatObservation, PreparedStatement) => BoundStatement = (observation: FloatObservation, prepared: PreparedStatement) => {
    val baseBound = prepared.bind()
      .setString("metric", observation.metric)
      .setString("shard", sharder.shard(observation.datetime))
      .setTimestamp("datetime", java.sql.Timestamp.valueOf(observation.datetime))
      .setFloat("value", observation.value)

    val folder: (BoundStatement, (String, String)) => BoundStatement = (prev: BoundStatement, tag: (String, String)) => prev.setString(tag._1, tag._2)

    observation.tagset.foldLeft(baseBound)(folder)
  }

  val getRawInsertPreparedStatement: FloatObservation => PreparedStatement = {

    val cache = CacheBuilder.newBuilder()
      .maximumSize(100)
      .build(
        new CacheLoader[Set[String], PreparedStatement] {
          def load(tags: Set[String]): PreparedStatement = {
            val tagnames = tags.toList
            val tagMarkers = tags.map(tag => bindMarker(tag)).toList

            val baseStatement = insertInto("tsorage_raw", "numeric")
              .value("metric", bindMarker("metric"))
              .value("shard", bindMarker("shard"))
              .value("datetime", bindMarker("datetime"))
              .value("value", bindMarker("value"))

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
  val notifyTagnames: (FloatMessage => FloatMessage) = {

    var cache: Set[String] = Set()

    val f: FloatMessage => FloatMessage = msg => {
      val recentTags = msg.tagset.keySet.filter(t => !(cache contains t))
      cache = cache ++ recentTags

      recentTags.map(tag => s"""ALTER TABLE tsorage_raw.numeric ADD "${tag.replace("\"", "\"\"")}" text;""")
        .foreach(t => Try(session.execute(t)))

      recentTags.map(tag => s"""ALTER TABLE tsorage_agg.numeric ADD "${tag.replace("\"", "\"\"")}" text;""")
        .foreach(t => Try(session.execute(t)))

      msg
    }

    f
  }

  val rawFlow = AltCassandraFlow.createWithPassThrough[FloatObservation](16,
    getRawInsertPreparedStatement,
    bindRawInsert)(session)
}
