package be.cetic.tsorage.processor.flow

import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL}
import be.cetic.tsorage.processor.aggregator.data.{CountAggregation, DataAggregation, SupportedValue}
import be.cetic.tsorage.processor.aggregator.data.tdouble.{MaximumAggregation, MinimumAggregation, SumAggregation}
import be.cetic.tsorage.processor.{AggUpdate, DAO, RawUpdate}
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.datatype.{DataTypeSupport, DoubleSupport, LongSupport}
import be.cetic.tsorage.processor.flow.FirstAggregationProcessingGraphFactory.{logger, processRawUpdate}
import com.datastax.driver.core.{ConsistencyLevel, SimpleStatement}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContextExecutor
import scala.collection.JavaConverters._


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
   private def processAggUpdate[A <: SupportedValue[A]](timeAggregator: TimeAggregator)(update: AggUpdate): AggUpdate = {

      val support: DataTypeSupport[_] = update.`type` match {
         case "double" => DoubleSupport
         case "long" => LongSupport
      }

      support.prepareAggUpdate(update, timeAggregator)
   }

   /**
     *
     * @param timeAggregator Describes the time level of aggregation.
     * @param context
     * @return A graph representing how the raw values are aggregated to provide aggregated values.
     */
   def createGraph(timeAggregator: TimeAggregator)(implicit context: ExecutionContextExecutor) = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      // Define internal flow shapes

      val worker = builder.add(Flow.fromFunction(processAggUpdate(timeAggregator)))

      // Combine shapes into a graph
      worker

      // Expose port
      FlowShape(worker.in, worker.out)
   }.named(s"Follow-up Aggregation Processing with ${timeAggregator.name}")

}
