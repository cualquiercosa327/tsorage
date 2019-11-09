package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.SinkShape
import akka.stream.alpakka.cassandra.scaladsl.CassandraSink
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Source}
import be.cetic.tsorage.common.sharder.Sharder
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.update.{DynamicTagUpdate, ShardedDynamicTagUpdate}
import be.cetic.tsorage.processor.{Message, ProcessorConfig}
import com.datastax.driver.core.{BoundStatement, ConsistencyLevel, PreparedStatement}
import com.datastax.driver.core.querybuilder.QueryBuilder

import scala.concurrent.ExecutionContextExecutor
import collection.JavaConverters._

class CassandraFlow(sharder: Sharder)(implicit val ec: ExecutionContextExecutor) {

  private val config = ProcessorConfig.conf

  private val rawKeyspace = config.getString("cassandra.keyspaces.raw")
  private val aggKeyspace = config.getString("cassandra.keyspaces.aggregated")

  private implicit val session = Cassandra.session

  private def dynamicStatement(table: String) = session.prepare(
    QueryBuilder.insertInto(aggKeyspace, table)
       .value("metric", QueryBuilder.bindMarker("metric"))
       .value("tagname", QueryBuilder.bindMarker("tagname"))
       .value("tagvalue", QueryBuilder.bindMarker("tagvalue"))
       .value("tagset", QueryBuilder.bindMarker("tagset"))
  ).setConsistencyLevel(ConsistencyLevel.LOCAL_ONE)

  private val dynamicTagBinder: (DynamicTagUpdate, PreparedStatement) => BoundStatement =
    (update, stm) => stm.bind()
       .setString("metric", update.metric)
       .setString("tagname", update.tagname)
       .setString("tagvalue", update.tagvalue)
       .setMap("tagset", update.tagset.asJava)

  private def shardedDynamicStatement(table: String) = session.prepare(
    QueryBuilder.insertInto(aggKeyspace, table)
       .value("metric", QueryBuilder.bindMarker("metric"))
       .value("shard", QueryBuilder.bindMarker("shard"))
       .value("tagname", QueryBuilder.bindMarker("tagname"))
       .value("tagvalue", QueryBuilder.bindMarker("tagvalue"))
       .value("tagset", QueryBuilder.bindMarker("tagset"))
  ).setConsistencyLevel(ConsistencyLevel.LOCAL_ONE)

  private val shardedDynamicTagBinder: (ShardedDynamicTagUpdate, PreparedStatement) => BoundStatement =
    (update, stm) => stm.bind()
       .setString("metric", update.metric)
       .setString("shard", update.shard)
       .setString("tagname", update.tagname)
       .setString("tagvalue", update.tagvalue)
       .setMap("tagset", update.tagset.asJava)

  /**
   * Converts messages into (tagname, tagvalue, message).
   * @return
   */
  private def splitTagset =
    Flow[Message].mapConcat(message => message.tagset.map(tag => DynamicTagUpdate(message.metric, tag._1, tag._2, message.tagset)))

  /**
   * Converts messages into (tagname, tagvalue, shard, message).
   * @return
   */
  private def splitShardedTagset =
    Flow[Message].mapConcat(message => {
      val shards = message.values.map(v => Cassandra.sharder.shard(v._1)).distinct

      shards.flatMap(shard => message.tagset.map(tag => ShardedDynamicTagUpdate(message.metric, shard, tag._1, tag._2, message.tagset)))
    })

  /**
    * A Cassandra sink for declaring the dynamic tag names and values
    */
  private def notifyDynamicTags =
  {
    val sink = CassandraSink(
      config.getInt("parallelism"),
      dynamicStatement("dynamic_tagset"),
      dynamicTagBinder
    )

    Flow[DynamicTagUpdate].to(sink)
  }

  private def notifyShardedDynamicTags =
  {
    val sink = CassandraSink(
      config.getInt("parallelism"),
      shardedDynamicStatement("sharded_dynamic_tagset"),
      shardedDynamicTagBinder
    )

    Flow[ShardedDynamicTagUpdate].to(sink)
  }

  private def notifyReverseDynamicTags =
  {
    val sink = CassandraSink(
      config.getInt("parallelism"),
      dynamicStatement("reverse_dynamic_tagset"),
      dynamicTagBinder
    )

    Flow[DynamicTagUpdate].to(sink)
  }

  private def notifyShardedReverseDynamicTags =
  {
    val sink = CassandraSink(
      config.getInt("parallelism"),
      shardedDynamicStatement("reverse_sharded_dynamic_tagset"),
      shardedDynamicTagBinder
    )

    Flow[ShardedDynamicTagUpdate].to(sink)
  }

  def tagIndexGraph(implicit context: ExecutionContextExecutor) = GraphDSL.create()
  {
    implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._

    val bcast = builder.add(Broadcast[Message](2))

    val tagSplitter = builder.add(splitTagset)
    val shardTagSplitter = builder.add(splitShardedTagset)

    bcast ~> tagSplitter ~> notifyReverseDynamicTags
    bcast ~> shardTagSplitter ~> notifyShardedReverseDynamicTags

    SinkShape[Message](bcast.in)
  }
}
