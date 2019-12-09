package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL}
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.{Message, ProcessorConfig}

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
      val bcast = builder.add(Broadcast[Message](2).async)
      val declareTags = cassandraFlow.tagIndexGraph

      val writeMessages = builder.add(CassandraWriter.createMessageCassandraFlow(context))

      // Combine shapes into a graph
      notEmpty ~> dropBadTags ~> bcast ~> writeMessages
                                 bcast ~> declareTags

      // Expose ports
      FlowShape(notEmpty.in, writeMessages.out)
   }.named("Raw Processing")
}
