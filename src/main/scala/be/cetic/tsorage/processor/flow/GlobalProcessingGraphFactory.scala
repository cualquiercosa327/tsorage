package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL}
import be.cetic.tsorage.processor.ObservationUpdate
import be.cetic.tsorage.processor.aggregator.TimeAggregator

import scala.concurrent.ExecutionContextExecutor

/**
  * A factory for preparing the global processing graph
  */
object GlobalProcessingGraphFactory
{
   def createGraph(aggregators: List[TimeAggregator])(implicit context: ExecutionContextExecutor) = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      // Define internal flow shapes

      val rawProcessor = builder.add(Flow.fromGraph(RawProcessingGraphFactory.createGraph))

      val aggregatorShapes = aggregators.map(aggregator => builder.add(Flow.fromGraph(AggregationProcessingGraphFactory.createGraph(aggregator))))

      var previous: FlowShape[_, ObservationUpdate[Float]] = rawProcessor

      // Combine shapes into a graph
      aggregatorShapes.foreach(agg => {
         previous ~> agg;
         previous = agg;
      })

      // Expose ports
      FlowShape(rawProcessor.in, previous.out)
   }
}
