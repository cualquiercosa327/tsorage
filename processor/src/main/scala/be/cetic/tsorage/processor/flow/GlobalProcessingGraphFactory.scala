package be.cetic.tsorage.processor.flow

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge}
import be.cetic.tsorage.common.TimeSeries
import be.cetic.tsorage.processor.{Message, Observation}
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.datatype.DataTypeSupport
import be.cetic.tsorage.processor.update.{AggUpdate, TimeAggregatorRawUpdate}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

/**
  * A factory for preparing the global processing graph
  */
object GlobalProcessingGraphFactory extends LazyLogging
{
   /*
   def createGraph(timeAggregators: List[TimeAggregator])(implicit context: ExecutionContextExecutor) = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val cassandraFlow = new CassandraFlow(Cassandra.sharder)

      // A - Check whether a support is available for this message; log invalid types
      val messageValidation = builder.add(Flow[Message]
         .map(msg => DataTypeSupport.availableSupports.get(msg.`type`) match {
            case None => { logger.warn(s"No support found for ${msg}"); None }
            case Some(support) => Some(msg)
         }).filter(_.isDefined).map(_.get))

      FlowShape(messageValidation.in, messageValidation.out)
   }
    */

   def createGraph(timeAggregators: List[TimeAggregator])(implicit context: ExecutionContextExecutor) = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      // Define internal flow shapes

      val rawProcessor = builder.add(Flow.fromGraph(RawProcessingGraphFactory.createGraph))

      if(timeAggregators.isEmpty)
      {
         FlowShape(rawProcessor.in, rawProcessor.out)
      }
      else
      {
         val firstAggregator = timeAggregators.head
         val aggCassandraFlow = CassandraWriter.createAggCassandraFlow

         val timeAggUpdate = builder.add(
            Flow[Message].mapConcat(message =>
               message.values
                  .map(v => firstAggregator.shunk(v._1))
                  .toSet
                  .map(shunk => TimeAggregatorRawUpdate(TimeSeries(message.metric, message.tagset), shunk, message.`type`)) )
         )

         val buffer = builder.add(
            Flow[TimeAggregatorRawUpdate]
               .groupedWithin(1000, 2.minutes)
               .mapConcat(_.distinct)
         )

         val firstAggregation = builder.add(FirstAggregationProcessingGraphFactory.createGraph(firstAggregator))
         val firstAggCassandraWriter = builder.add(aggCassandraFlow)

         rawProcessor ~> timeAggUpdate ~> buffer ~> firstAggregation ~> firstAggCassandraWriter

         var previous = firstAggCassandraWriter

         val followUpAggregators = timeAggregators.tail

         val aggregatorShapes = followUpAggregators.map(aggregator =>
            builder.add(Flow.fromGraph(FollowUpAggregationProcessingGraphFactory.createGraph(aggregator)).async))

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
