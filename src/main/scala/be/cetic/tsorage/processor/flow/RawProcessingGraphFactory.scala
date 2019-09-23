package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.{ActorMaterializer, Attributes, ClosedShape, FlowShape, Inlet, Outlet}
import akka.stream.scaladsl.{Flow, GraphDSL}
import akka.stream.stage.{GraphStage, GraphStageLogic}
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.{Message, Observation, ObservationUpdate, ProcessorConfig}
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

      val notEmpty = builder.add(Flow[Message[Any]].filter(message => !message.values.isEmpty))
      val dropBadTags = builder.add(Flow[Message[Any]].map(ProcessorConfig.dropBadTags))
      val declareTags = builder.add(Flow[Message[Any]].map(cassandraFlow.notifyTagnames))
      val flattenMessage = builder.add(ObservationFlow.flattenMessage[Any])
      val storeRawValues = builder.add(cassandraFlow.rawFlow[Any])
      val toUpdate = builder.add(
         Flow.fromFunction({observation: Observation[Any] => ObservationUpdate(
            observation.metric,
            observation.tagset,
            observation.datetime,
            "raw",
            Map("raw" -> observation.value),
            observation.support
         )})
      )

      // Combine shapes into a graph
      notEmpty ~> dropBadTags ~> declareTags ~> flattenMessage ~> storeRawValues ~> toUpdate

      // Expose ports
     FlowShape(notEmpty.in, toUpdate.out)
   }.named("Raw Processing")
}
