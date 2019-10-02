package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL}
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
      val declareTags = builder.add(Flow[Message].map(cassandraFlow.notifyTagnames))
      val flatten = builder.add(Flow[Message].mapConcat(ObservationFlow.messageToRawUpdates))

      // Combine shapes into a graph
      notEmpty ~> dropBadTags ~> declareTags ~> flatten

      // Expose ports
     FlowShape(notEmpty.in, flatten.out)
   }.named("Raw Processing")
}
