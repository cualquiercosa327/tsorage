package be.cetic.tsorage.processor.flow

import be.cetic.tsorage.common.sharder.Sharder
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.{Message, ProcessorConfig}

import scala.concurrent.ExecutionContextExecutor
import scala.util.Try

class CassandraFlow(sharder: Sharder)(implicit val ec: ExecutionContextExecutor) {

  private val config = ProcessorConfig.conf

  private val rawKeyspace = config.getString("cassandra.keyspaces.raw")
  private val aggKeyspace = config.getString("cassandra.keyspaces.aggregated")

  private implicit val session = Cassandra.session

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

      recentTags.map(tag => s"""ALTER TABLE ${rawKeyspace}.observations ADD "${tag.replace("\"", "\"\"")}" text;""")
        .foreach(t => Try(session.execute(t)))

      recentTags.map(tag => s"""ALTER TABLE ${aggKeyspace}.observations ADD "${tag.replace("\"", "\"\"")}" text;""")
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
