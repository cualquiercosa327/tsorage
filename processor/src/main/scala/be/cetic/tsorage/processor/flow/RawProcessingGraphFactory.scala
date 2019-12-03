package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.alpakka.cassandra.scaladsl.CassandraSink
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL}
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.update.DynamicTagUpdate
import be.cetic.tsorage.processor.{Message, ProcessorConfig}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor

/**
  * A factory for raw value processing
  */
object RawProcessingGraphFactory
{
   def createGraph(implicit context: ExecutionContextExecutor) = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      // Define internal flow shapes

      val cassandraFlow = new CassandraFlow(Cassandra.sharder)

      val notEmpty = builder.add(Flow[Message].filter(message => !message.values.isEmpty))
      val dropBadTags = builder.add(Flow[Message].map(ProcessorConfig.dropBadTags))
      val bcast = builder.add(Broadcast[Message](3).async)
      val declareTags = builder.add(Flow[Message].map(cassandraFlow.notifyTagnames))   // Add the tag columns

      // Update the dynamic tags
      val updateDynamicIndices = builder.add(DynamicTagUpdateGraphFactory.createDynamicTagUpdater(ProcessorConfig.conf.getInt("parallelism")))
      val updateShardedDynamicIndices = builder.add(DynamicTagUpdateGraphFactory.createShardedDynamicTagUpdater(ProcessorConfig.conf.getInt("parallelism")))

      val writeMessages = builder.add(CassandraWriter.createMessageCassandraFlow(context))

      // Combine shapes into a graph
      notEmpty ~> dropBadTags ~> bcast ~> declareTags ~> writeMessages
                                 bcast ~> updateDynamicIndices
                                 bcast ~> updateShardedDynamicIndices

      // Expose ports
     FlowShape(notEmpty.in, writeMessages.out)
   }.named("Raw Processing")
}
