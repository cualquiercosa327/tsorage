package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.{FlowShape, SinkShape}
import akka.stream.alpakka.cassandra.scaladsl.CassandraSink
import akka.stream.scaladsl.{Flow, GraphDSL, Sink}
import be.cetic.tsorage.processor.Message
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.update.{DynamicTagUpdate, ShardedDynamicTagUpdate}
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.datastax.driver.core.querybuilder.QueryBuilder

import scala.concurrent.ExecutionContextExecutor

/**
 * This graph converts messages into dynamic tag index updates,
 */
object DynamicTagUpdateGraphFactory
{
   def createDynamicTagUpdater(parallelism: Int)(implicit context: ExecutionContextExecutor) = GraphDSL.create()
   {
      implicit builder: GraphDSL.Builder[NotUsed] =>
      {
         import GraphDSL.Implicits._

         val globalStatement = Cassandra.session.prepare(
            QueryBuilder.insertInto(Cassandra.aggKS, "dynamic_tagset")
               .value("metric", QueryBuilder.bindMarker("metric"))
               .value("tagname", QueryBuilder.bindMarker("tagname"))
               .value("tagvalue", QueryBuilder.bindMarker("tagvalue"))
         )

         def globalBinder(update: DynamicTagUpdate, stm: PreparedStatement): BoundStatement =
            stm.bind()
               .setString("metric", update.metric)
               .setString("tagname", update.tagname)
               .setString("tagvalue", update.tagvalue)

         val toDynamicTagUpdate =
            builder.add(Flow[Message].mapConcat(message => message.tagset.map(tag => DynamicTagUpdate(message.metric, tag._1, tag._2))))

         val sink = builder.add(CassandraSink(parallelism, globalStatement, globalBinder)(Cassandra.session))

         toDynamicTagUpdate ~> sink

         SinkShape(toDynamicTagUpdate.in)
      }
   }

   def createShardedDynamicTagUpdater(parallelism: Int)(implicit context: ExecutionContextExecutor) = GraphDSL.create()
   {
      implicit builder: GraphDSL.Builder[NotUsed] =>
      {
         import GraphDSL.Implicits._
         val shardStatement = Cassandra.session.prepare(
            QueryBuilder.insertInto(Cassandra.aggKS, "sharded_dynamic_tagset")
               .value("metric", QueryBuilder.bindMarker("metric"))
               .value("shard", QueryBuilder.bindMarker("shard"))
               .value("tagname", QueryBuilder.bindMarker("tagname"))
               .value("tagvalue", QueryBuilder.bindMarker("tagvalue"))
         )

         def shardBinder(update: ShardedDynamicTagUpdate, stm: PreparedStatement): BoundStatement =
            stm.bind()
               .setString("metric", update.metric)
               .setString("shard", update.shard)
               .setString("tagname", update.tagname)
               .setString("tagvalue", update.tagvalue)

         val sharder = Cassandra.sharder

         val toShardedDynamicTagUpdate =
            builder.add(
               Flow[Message].mapConcat(message => {
                  message
                     .values
                     .map(v => sharder.shard(v._1))
                     .distinct
                     .flatMap(shard => message.tagset.map(tag => ShardedDynamicTagUpdate(message.metric, shard, tag._1, tag._2)))
               })
            )

         val sink = builder.add(CassandraSink(parallelism, shardStatement, shardBinder)(Cassandra.session))

         toShardedDynamicTagUpdate ~> sink

         SinkShape(toShardedDynamicTagUpdate.in)
      }
   }
}
