package be.cetic.tsorage.processor.flow

import java.time.LocalDateTime
import java.util.Date

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL}
import be.cetic.tsorage.processor.aggregator.data.CountAggregation
import be.cetic.tsorage.processor.{AggUpdate, DAO, RawUpdate}
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.datatype.{DataTypeSupport, DoubleSupport, LongSupport}
import com.datastax.driver.core.{ConsistencyLevel, SimpleStatement}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContextExecutor
import scala.collection.JavaConverters._

/**
  * A factory for creating processing graph handling the first aggregation.
  *
  * Input: RawUpdate, alerting a new raw value has been added to the system.
  * Output: AggUpdate, alerting an aggregated value has been updated.
  */
object FirstAggregationProcessingGraphFactory extends LazyLogging
{
   /**
     * Aggregates raw values and stores them to Cassandra.
     * @param timeAggregator The aggregator specifying how time must be aggregated.
     * @param update A representation of a stored/updated raw value.
     * @return objects representing the aggregated values that have been updated.
     */
   private def processRawUpdate(timeAggregator: TimeAggregator)(update: RawUpdate): List[AggUpdate] =
   {
      val support = DataTypeSupport.inferSupport(update)

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

      val worker = builder.add(Flow.fromFunction(processRawUpdate(timeAggregator)))
      val flattener = builder.add(Flow[List[AggUpdate]].mapConcat(identity))

      // Combine shapes into a graph
      worker ~> flattener

      // Expose port
      FlowShape(worker.in, flattener.out)
   }.named(s"First Aggregation Processing with ${timeAggregator.name}")

}
