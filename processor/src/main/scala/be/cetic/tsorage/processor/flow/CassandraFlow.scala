package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.SinkShape
import akka.stream.alpakka.cassandra.CassandraBatchSettings
import akka.stream.alpakka.cassandra.scaladsl.{CassandraFlow, CassandraSink}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source}
import be.cetic.tsorage.common.sharder.Sharder
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.update.{ShardedMessageUpdate, ShardedTagUpdate, TagUpdate}
import be.cetic.tsorage.processor.{Message, ProcessorConfig}
import com.datastax.driver.core.{BoundStatement, ConsistencyLevel, PreparedStatement}
import com.datastax.driver.core.querybuilder.QueryBuilder

import scala.concurrent.ExecutionContextExecutor
import collection.JavaConverters._
import scala.concurrent.duration._


class CassandraFlow(sharder: Sharder)(implicit val ec: ExecutionContextExecutor) {

  private val config = ProcessorConfig.conf

  private val rawKeyspace = config.getString("cassandra.keyspaces.raw")
  private val aggKeyspace = config.getString("cassandra.keyspaces.aggregated")

  private implicit val session = Cassandra.session

  private val dynamicStatement = session.prepare(
    QueryBuilder.insertInto(aggKeyspace, "dynamic_tagset")
       .value("metric", QueryBuilder.bindMarker("metric"))
       .value("tagset", QueryBuilder.bindMarker("tagset"))
  ).setConsistencyLevel(ConsistencyLevel.LOCAL_ONE)

  private val dynamicTagBinder: (Message, PreparedStatement) => BoundStatement =
    (update, stm) => stm.bind()
       .setString("metric", update.metric)
       .setMap("tagset", update.tagset.asJava)

  private val reverseDynamicStatement = session.prepare(
    QueryBuilder.insertInto(aggKeyspace, "reverse_dynamic_tagset")
       .value("metric", QueryBuilder.bindMarker("metric"))
       .value("tagname", QueryBuilder.bindMarker("tagname"))
       .value("tagbalue", QueryBuilder.bindMarker("tagvalue"))
       .value("tagset", QueryBuilder.bindMarker("tagset"))
  ).setConsistencyLevel(ConsistencyLevel.LOCAL_ONE)

  private val reverseDynamicTagBinder: (TagUpdate, PreparedStatement) => BoundStatement =
    (update, stm) => stm.bind()
       .setString("metric", update.metric)
       .setString("tagname", update.tagname)
       .setString("tagvalue", update.tagvalue)
       .setMap("tagset", update.tagset.asJava)

  private val shardedDynamicStatement = session.prepare(
    QueryBuilder.insertInto(aggKeyspace, "sharded_dynamic_tagset")
       .value("metric", QueryBuilder.bindMarker("metric"))
       .value("shard", QueryBuilder.bindMarker("shard"))
       .value("tagset", QueryBuilder.bindMarker("tagset"))
  ).setConsistencyLevel(ConsistencyLevel.LOCAL_ONE)

  private val shardedDynamicTagBinder: (ShardedMessageUpdate, PreparedStatement) => BoundStatement =
    (update, stm) => stm.bind()
       .setString("metric", update.metric)
       .setString("shard", update.shard)
       .setMap("tagset", update.tagset.asJava)

  private val shardedReverseDynamicStatement = session.prepare(
    QueryBuilder.insertInto(aggKeyspace, "reverse_sharded_dynamic_tagset")
       .value("metric", QueryBuilder.bindMarker("metric"))
       .value("shard", QueryBuilder.bindMarker("shard"))
       .value("tagset", QueryBuilder.bindMarker("tagset"))
       .value("tagname", QueryBuilder.bindMarker("tagname"))
       .value("tagvalue", QueryBuilder.bindMarker("tagvalue"))
  )

  private val shardedReverseDynamicTagBinder: (ShardedTagUpdate, PreparedStatement) => BoundStatement =
    (update, stm) => stm.bind()
       .setString("metric", update.metric)
       .setString("shard", update.shard)
       .setString("tagname", update.tagname)
       .setString("tagvalue", update.tagvalue)
       .setMap("tagset", update.tagset.asJava)

  /**
   * Converts messages into (metric, tagname, tagvalue, tagset).
   */
  private def splitByTag =
    Flow[Message].mapConcat(message => message.tagset.map(tag => TagUpdate(message.metric, tag._1, tag._2, message.tagset)))

  /**
   * Converts messages into (metric, shard, tagset).
   */
  private def splitByShard =
    Flow[Message].mapConcat(message => message.values.map(v =>
      sharder.shard(v._1)).toSet.map(shard => ShardedMessageUpdate(message.metric, shard, message.tagset)))


  /**
   * Converts messages into (metric, shard, tagname, tagvalue, tagset).
   */
  private def splitByTagAndShard =
    Flow[Message].mapConcat(message => {
      val shards = message.values.map(v => sharder.shard(v._1)).distinct

      shards.flatMap(shard => message.tagset.map(tag => ShardedTagUpdate(message.metric, shard, tag._1, tag._2, message.tagset)))
    })

  /**
    * A Cassandra sink for declaring the dynamic tag names and values
    */
  private def notifyDynamicTags =
  {
    val flow = CassandraFlow.createUnloggedBatchWithPassThrough[Message, String](
      8,
      dynamicStatement,
      dynamicTagBinder,
      (message => message.metric),
      CassandraBatchSettings(1000, 2.minutes)
    ).named("notify dynamic tags")

    flow.to(Sink.ignore)
  }

  /**
   * A Cassandra sink for declaring the tagsets associated with a particular metric.
   */
  private def notifyShardedDynamicTags =
  {
    val flow = CassandraFlow.createUnloggedBatchWithPassThrough[ShardedMessageUpdate, String](
      8,
      shardedDynamicStatement,
      shardedDynamicTagBinder,
      (update => update.metric + update.shard),
      CassandraBatchSettings(1000, 2.minutes)
    ).named("notify sharded dynamic tags")

    flow.to(Sink.ignore)
  }

  private def notifyReverseDynamicTags =
  {
    val flow = CassandraFlow.createUnloggedBatchWithPassThrough[TagUpdate, String](
      8,
      reverseDynamicStatement,
      reverseDynamicTagBinder,
      (update => update.tagname),
      CassandraBatchSettings(1000, 2.minutes)
    ).named("notify reverse dynamic tags")

    flow.to(Sink.ignore)
  }

  private def notifyShardedReverseDynamicTags =
  {
    val flow = CassandraFlow.createUnloggedBatchWithPassThrough[ShardedTagUpdate, String](
      8,
      shardedReverseDynamicStatement,
      shardedReverseDynamicTagBinder,
      (update => (update.shard + update.tagname)),
      CassandraBatchSettings(1000, 2.minutes)
    ).named("notify sharded reverse dynamic tags")

    flow.to(Sink.ignore)
  }

  def tagIndexGraph(implicit context: ExecutionContextExecutor) = GraphDSL.create()
  {
    implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._

    val bcast = builder.add(Broadcast[Message](4))

     bcast ~> notifyDynamicTags                         // Uses Message
     bcast ~> splitByTag ~> notifyReverseDynamicTags   // Uses TagUpdate
     bcast ~> splitByShard ~> notifyShardedDynamicTags // Uses ShardedMessageUpdate
     bcast ~> splitByTagAndShard ~> notifyShardedReverseDynamicTags // Uses ShardedTagUpdate

    SinkShape[Message](bcast.in)
  }
}
