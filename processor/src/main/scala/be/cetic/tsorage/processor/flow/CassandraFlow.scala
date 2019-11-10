package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.SinkShape
import akka.stream.alpakka.cassandra.CassandraBatchSettings
import akka.stream.alpakka.cassandra.scaladsl.{CassandraFlow, CassandraSink}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source}
import be.cetic.tsorage.common.sharder.Sharder
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.update.{DynamicTagUpdate, ShardedDynamicTagUpdate}
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
    val flow = CassandraFlow.createUnloggedBatchWithPassThrough[DynamicTagUpdate, String](
      8,
      dynamicStatement("dynamic_tagset"),
      dynamicTagBinder,
      (update => update.metric),
      CassandraBatchSettings(1000, 2.minutes)
    ).named("notify dynamic tags")

    flow.to(Sink.ignore)

    /*
    val sink = CassandraSink(
      config.getInt("parallelism"),
      dynamicStatement("dynamic_tagset"),
      dynamicTagBinder
    )

    Flow[DynamicTagUpdate].to(sink)

     */
  }

  /**
   * A Cassandra sink for declaring the tagsets associated with a particular metric.
   */
  private def notifyShardedDynamicTags =
  {
    val flow = CassandraFlow.createUnloggedBatchWithPassThrough[ShardedDynamicTagUpdate, String](
      8,
      shardedDynamicStatement("sharded_dynamic_tagset"),
      shardedDynamicTagBinder,
      (update => update.metric + update.shard),
      CassandraBatchSettings(1000, 2.minutes)
    ).named("notify sharded dynamic tags")

    flow.to(Sink.ignore)

    /*
    val sink = CassandraSink(
      config.getInt("parallelism"),
      shardedDynamicStatement("sharded_dynamic_tagset"),
      shardedDynamicTagBinder
    )

    Flow[ShardedDynamicTagUpdate].to(sink)

     */
  }

  private def notifyReverseDynamicTags =
  {
/*
    val sink = CassandraSink(
      config.getInt("parallelism"),
      dynamicStatement("reverse_dynamic_tagset"),
      dynamicTagBinder
    )

    Flow[DynamicTagUpdate].to(sink)
*/

    val flow = CassandraFlow.createUnloggedBatchWithPassThrough[DynamicTagUpdate, String](
      8,
      dynamicStatement("reverse_dynamic_tagset"),
      dynamicTagBinder,
      (update => update.tagname),
      CassandraBatchSettings(1000, 2.minutes)
    ).named("notify reverse dynamic tags")

    flow.to(Sink.ignore)

  }

  private def notifyShardedReverseDynamicTags =
  {
    /*
    val sink = CassandraSink(
      config.getInt("parallelism"),
      shardedDynamicStatement("reverse_sharded_dynamic_tagset"),
      shardedDynamicTagBinder
    )

    Flow[ShardedDynamicTagUpdate].to(sink)
     */

    val flow = CassandraFlow.createUnloggedBatchWithPassThrough[ShardedDynamicTagUpdate, String](
      8,
      shardedDynamicStatement("reverse_sharded_dynamic_tagset"),
      shardedDynamicTagBinder,
      (update => (update.shard + update.tagname)),
      CassandraBatchSettings(1000, 2.minutes)
    ).named("notify sharded reverse dynamic tags")

    flow.to(Sink.ignore)
  }

  def tagIndexGraph(implicit context: ExecutionContextExecutor) = GraphDSL.create()
  {
    implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._

    val bcast = builder.add(Broadcast[Message](2))

    val tagSplitter = builder.add(splitTagset.async)
    val bcastDynamicTag = builder.add(Broadcast[DynamicTagUpdate](2))
    val tagUpdateBuffer = builder.add(
      Flow[DynamicTagUpdate]
         .groupedWithin(1000, 2.minutes)
         .mapConcat(_.distinct)
    )

    val shardTagSplitter = builder.add(splitShardedTagset)
    val bcastShardedDynamicTag = builder.add(Broadcast[ShardedDynamicTagUpdate](2))

    val shardedTagUpdateBuffer = builder.add(
      Flow[ShardedDynamicTagUpdate]
         .groupedWithin(1000, 2.minutes)
         .mapConcat(_.distinct)
    )


    bcast ~> tagSplitter ~> tagUpdateBuffer ~> bcastDynamicTag ~> notifyReverseDynamicTags
                                               bcastDynamicTag ~> notifyDynamicTags

      bcast ~> shardTagSplitter ~> shardedTagUpdateBuffer ~> bcastShardedDynamicTag ~> notifyShardedReverseDynamicTags
                                                             bcastShardedDynamicTag ~> notifyShardedDynamicTags

    SinkShape[Message](bcast.in)
  }
}
