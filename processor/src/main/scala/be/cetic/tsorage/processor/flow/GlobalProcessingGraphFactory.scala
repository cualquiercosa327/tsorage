package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL}
import be.cetic.tsorage.processor.aggregator.time.{HourAggregator, MinuteAggregator, TimeAggregator}
import be.cetic.tsorage.processor.update.{AggUpdate, RawUpdate}

import scala.concurrent.ExecutionContextExecutor

/**
  * A factory for preparing the global processing graph
  */
object GlobalProcessingGraphFactory
{
   def createGraph(timeAggregators: List[TimeAggregator])(implicit context: ExecutionContextExecutor) = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val rawCassandraFlow = CassandraWriter.createRawCassandraFlow
      val aggCassandraFlow = CassandraWriter.createAggCassandraFlow

      // Define internal flow shapes

      val rawProcessor = builder.add(Flow.fromGraph(RawProcessingGraphFactory.createGraph))
      val rawCassandraWriter = builder.add(rawCassandraFlow)

      rawProcessor ~> rawCassandraWriter

      if(timeAggregators.isEmpty)
      {
         val out = builder.add(Flow[RawUpdate].log(name = "event processing"))

         rawCassandraWriter ~> out
         FlowShape(rawProcessor.in, out.out)
      }
      else
      {
         val firstAggregation = builder.add(FirstAggregationProcessingGraphFactory.createGraph(timeAggregators.head))
         val firstAggCassandraWriter = builder.add(aggCassandraFlow)

         rawCassandraWriter ~> firstAggregation ~> firstAggCassandraWriter

         var previous = firstAggCassandraWriter

         val followUpAggregators = timeAggregators.tail

         val aggregatorShapes = followUpAggregators.map(aggregator =>
            builder.add(Flow.fromGraph(FollowUpAggregationProcessingGraphFactory.createGraph(aggregator))))

         // Combine shapes into a graph
         aggregatorShapes.foreach(agg => {
            val cassandraWriter = builder.add(aggCassandraFlow)

            previous ~> agg ~> cassandraWriter;
            previous = cassandraWriter;
         })

         val out = builder.add(Flow[AggUpdate].log(name = "myStream"))

         previous ~> out
         FlowShape(rawProcessor.in, out.out)
      }
   }
}
