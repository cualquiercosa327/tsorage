package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.{ActorMaterializer, Attributes, ClosedShape, FlowShape, Inlet, Outlet}
import akka.stream.scaladsl.{Flow, GraphDSL}
import akka.stream.stage.{GraphStage, GraphStageLogic}
import be.cetic.tsorage.processor.aggregator.data.SupportedValue
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.{Message, Observation, ProcessorConfig, RawUpdate}
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.sharder.Sharder

import scala.concurrent.{ExecutionContextExecutor, Future}

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
