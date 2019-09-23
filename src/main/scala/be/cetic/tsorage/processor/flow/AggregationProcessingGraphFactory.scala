package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL}
import be.cetic.tsorage.processor.ObservationUpdate
import be.cetic.tsorage.processor.aggregator.Aggregator
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator

import scala.concurrent.ExecutionContextExecutor

/**
  * A graph factory for processing aggregated values
  */
object AggregationProcessingGraphFactory
{
   private def prepareValue[T](timeAggregator: TimeAggregator)(update: ObservationUpdate[T]) = {
      val shunkedUpdate = timeAggregator.shunk(update)
      val aggregator = Aggregator(timeAggregator, update.support)

      aggregator.updateShunk(shunkedUpdate)
      shunkedUpdate
   }

   def createGraph(timeAggregator: TimeAggregator)(implicit context: ExecutionContextExecutor) = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      // Define internal flow shapes

      val worker = builder.add(Flow.fromFunction(prepareValue[Any](timeAggregator)))

      // Combine shapes into a graph
      worker

      // Expose port
      FlowShape(worker.in, worker.out)
   }.named(s"Aggregation Processing ${timeAggregator.name}")
}
