package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL}
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.datatype.DataTypeSupport
import be.cetic.tsorage.processor.update.AggUpdate
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContextExecutor


/**
  * A factory for creating processing graph handling the aggregations following the first one.
  * For the factory managing the graph responsible of the first aggregation, see FirstAggregationProcessingGraphFactory.
  *
  * Input: AggUpdate, informing an aggregated value has been created/updated in the system.
  * Output: AggUpdate, informing an aggregated value has been updated.
  */
object FollowUpAggregationProcessingGraphFactory  extends LazyLogging
{
   /**
     * Aggregates aggregated values and stores them to Cassandra.
     * @param timeAggregator The aggregator specifying how time must be aggregated.
     * @param update A representation of a stored/updated aggregated value.
     * @return objects representing the aggregated values that have been updated.
     */
   private def processAggUpdate(timeAggregator: TimeAggregator)(update: AggUpdate): AggUpdate = {
      DataTypeSupport.inferSupport(update)prepareAggUpdate(update, timeAggregator)
   }

   /**
     *
     * @param timeAggregator Describes the time level of aggregation.
     * @param context
     * @return A graph representing how the raw values are aggregated to provide aggregated values.
     */
   def createGraph(timeAggregator: TimeAggregator)(implicit context: ExecutionContextExecutor) = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>

      // Define internal flow shapes

      val worker = builder.add(Flow.fromFunction(processAggUpdate(timeAggregator)))

      // Combine shapes into a graph
      worker

      // Expose port
      FlowShape(worker.in, worker.out)
   }.named(s"Follow-up Aggregation Processing with ${timeAggregator.name}")

}