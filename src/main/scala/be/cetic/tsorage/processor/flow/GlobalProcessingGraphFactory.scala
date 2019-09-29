package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL}
import be.cetic.tsorage.processor.AggUpdate
import be.cetic.tsorage.processor.aggregator.time.{HourAggregator, MinuteAggregator, TimeAggregator}

import scala.concurrent.ExecutionContextExecutor

/**
  * A factory for preparing the global processing graph
  */
object GlobalProcessingGraphFactory
{
   def createGraph(timeAggregators: List[TimeAggregator])(implicit context: ExecutionContextExecutor) = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      // Define internal flow shapes

      val rawProcessor = builder.add(Flow.fromGraph(RawProcessingGraphFactory.createGraph))
      val rawCassandraWriter = builder.add(CassandraWriter.createRawCassandraFlow)
      val firstAggregation = builder.add(FirstAggregationProcessingGraphFactory.createGraph(new MinuteAggregator("raw")))
      val firstAggCassandraWriter = builder.add(CassandraWriter.createAggCassandraFlow)
      val secondAggregation = builder.add(FollowUpAggregationProcessingGraphFactory.createGraph(new HourAggregator("1m")))
      val secondAggCassandraWriter = builder.add(CassandraWriter.createAggCassandraFlow)

      /*
            //val aggregatorShapes = timeAggregators.map(aggregator => builder.add(Flow.fromGraph(AggregationProcessingGraphFactory.createGraph(aggregator))))

            var previous: FlowShape[_, ObservationUpdate[Any]] = rawProcessor

            // Combine shapes into a graph
            aggregatorShapes.foreach(agg => {
               previous ~> agg;
               previous = agg;
            })
      */

      val out = builder.add(Flow[AggUpdate].log(name = "myStream"))

      rawProcessor ~> rawCassandraWriter ~>
         firstAggregation ~> firstAggCassandraWriter ~>
         secondAggregation ~> secondAggCassandraWriter ~>
         out
      // Expose ports
      FlowShape(rawProcessor.in, out.out)
   }
}
