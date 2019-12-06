package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL}
import be.cetic.tsorage.processor.ProcessorConfig
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.datatype.DataTypeSupport
import be.cetic.tsorage.processor.update.{AggUpdate, TimeAggregatorRawUpdate}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContextExecutor

/**
  * A factory for creating processing graph handling the first aggregation.
  *
  * Input: RawUpdate, alerting a new raw value has been added to the system.
  * Output: AggUpdate, alerting an aggregated value has been updated.
  */
object FirstAggregationProcessingGraphFactory extends LazyLogging
{
   /**
     *
     * @param timeAggregator Describes the time level of aggregation.
     * @param context
     * @return A graph representing how the raw values are aggregated to provide aggregated values.
     */
   def createGraph(timeAggregator: TimeAggregator)
                  (implicit context: ExecutionContextExecutor) =
      GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      // Define internal flow shapes

      val aggs = builder.add(
         Flow[TimeAggregatorRawUpdate].mapAsyncUnordered(ProcessorConfig.conf.getInt("parallelism"))(update => {
            val support = DataTypeSupport.inferSupport(update)
            support.prepareAggUpdate(update, timeAggregator)
         }
      ))

      val flattener = builder.add(Flow[List[AggUpdate]].mapConcat(identity))

       aggs ~> flattener

      // Expose port
      FlowShape(aggs.in, flattener.out)
   }.named(s"First Aggregation Processing with ${timeAggregator.name}")

}
