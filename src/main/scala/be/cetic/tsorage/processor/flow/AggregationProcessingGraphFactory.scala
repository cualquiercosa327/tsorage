package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL}
import be.cetic.tsorage.processor.aggregator.TimeAggregator
import be.cetic.tsorage.processor.{ObservationUpdate}

import scala.concurrent.ExecutionContextExecutor

/**
  * A graph factory for processing aggregated values
  */
object AggregationProcessingGraphFactory
{
   private def prepareValue(aggregator: TimeAggregator)(update: ObservationUpdate) = {
      val shunkedUpdate = aggregator.shunk(update)

      aggregator.updateShunk(shunkedUpdate)
      shunkedUpdate
   }

   def createGraph(aggregator: TimeAggregator)(implicit context: ExecutionContextExecutor) = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      // Define internal flow shapes

      val worker = builder.add(Flow.fromFunction(prepareValue(aggregator)))

      // Combine shapes into a graph
      worker

      // Expose port
      FlowShape(worker.in, worker.out)
   }.named(s"Aggregation Processing ${aggregator.name}")
}
