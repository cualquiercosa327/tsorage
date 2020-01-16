package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.SinkShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink}
import be.cetic.tsorage.common.messaging.{AggUpdate, InformationVector, Observation, ShardedInformationVector, ShardedTagUpdate, TagUpdate}
import be.cetic.tsorage.processor.ProcessorConfig
import be.cetic.tsorage.processor.update.ShardedTagsetUpdate
import com.datastax.driver.core.{BoundStatement, ConsistencyLevel, PreparedStatement, Session}
import com.datastax.driver.core.querybuilder.QueryBuilder

import scala.collection.JavaConverters._
import GraphDSL.Implicits._

import akka.stream.alpakka.cassandra.CassandraBatchSettings
import akka.stream.alpakka.cassandra.scaladsl.CassandraFlow

import scala.concurrent.ExecutionContextExecutor

/**
 * A flow box for processing shared tagset updates.
 */
case class TagUpdateBlock(session: Session)
{
   private val config = ProcessorConfig.conf

   private val aggKeyspace = config.getString("cassandra.keyspaces.aggregated")

   private val dynamicStatement = session.prepare(
      QueryBuilder.insertInto(aggKeyspace, "dynamic_tagset")
         .value("metric", QueryBuilder.bindMarker("metric"))
         .value("tagset", QueryBuilder.bindMarker("tagset"))
   ).setConsistencyLevel(ConsistencyLevel.LOCAL_ONE)

   private val dynamicTagBinder: (ShardedTagsetUpdate, PreparedStatement) => BoundStatement =
      (dp, stm) => stm.bind()
         .setString("metric", dp.metric)
         .setMap("tagset", dp.tagset.asJava)

   private val reverseDynamicStatement = session.prepare(
      QueryBuilder.insertInto(aggKeyspace, "reverse_dynamic_tagset")
         .value("metric", QueryBuilder.bindMarker("metric"))
         .value("tagname", QueryBuilder.bindMarker("tagname"))
         .value("tagvalue", QueryBuilder.bindMarker("tagvalue"))
         .value("tagset", QueryBuilder.bindMarker("tagset"))
   ).setConsistencyLevel(ConsistencyLevel.LOCAL_ONE)

   private val reverseDynamicTagBinder: (ShardedTagUpdate, PreparedStatement) => BoundStatement =
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

   private val shardedDynamicTagBinder: (ShardedTagsetUpdate, PreparedStatement) => BoundStatement =
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

   def createGraph(implicit context: ExecutionContextExecutor) = GraphDSL.create()
   {
      implicit builder: GraphDSL.Builder[NotUsed] =>

         val stubcast = builder.add(Broadcast[ShardedTagsetUpdate](3).async)
         val tuBcast = builder.add(Broadcast[ShardedTagUpdate](2).async)

         val notifyDynamicTagset = builder.add(
            CassandraFlow.createWithPassThrough(
               4,
               dynamicStatement,
               dynamicTagBinder
            )(session).named("notify dynamic tags")
         )

         val notityShardedDynamicTagset = builder.add(
            CassandraFlow.createWithPassThrough(
               4,
               shardedDynamicStatement,
               shardedDynamicTagBinder
            )(session).named("notify sharded dynamic tags")
         )

         val asShardedTagUpdate = builder.add(
            Flow[ShardedTagsetUpdate]
               .mapConcat(stu => stu.splitByTag.toList)
         )

         val notifyReverseDynamicTags = builder.add(
            CassandraFlow.createWithPassThrough(
               4,
               reverseDynamicStatement,
               reverseDynamicTagBinder
            )(session).named("notify reverse dynamic tags")
         )

         val notifyShardedReverseDynamicTags = builder.add(
            CassandraFlow.createWithPassThrough(
               4,
               shardedReverseDynamicStatement,
               shardedReverseDynamicTagBinder
            )(session).named("notify sharded reverse dynamic tags")
         )

         stubcast ~> notifyDynamicTagset ~> Sink.ignore
         stubcast ~> notityShardedDynamicTagset ~> Sink.ignore
         stubcast ~> asShardedTagUpdate ~> tuBcast ~> notifyReverseDynamicTags ~> Sink.ignore
                                           tuBcast ~> notifyShardedReverseDynamicTags ~> Sink.ignore

         SinkShape[ShardedTagsetUpdate](stubcast.in)
   }
}
