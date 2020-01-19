package be.cetic.tsorage.processor.flow

import java.time.{LocalDateTime, ZoneId}

import akka.NotUsed
import akka.stream.{FlowShape, OverflowStrategy, SinkShape, SourceShape, UniformFanOutShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source}
import GraphDSL.Implicits._
import be.cetic.tsorage.common.json.{AggUpdateJsonSupport, ObservationJsonSupport}
import be.cetic.tsorage.common.messaging.{AggUpdate, Observation}
import be.cetic.tsorage.processor.aggregator.raw.{RawAggregator, SimpleRawDerivator}
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.datatype.DataTypeSupport
import be.cetic.tsorage.processor.update.TimeAggregatorRawUpdate
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.{ExecutionContextExecutor, Future}
import spray.json._

private case class RawAggregationTrigger(ru: TimeAggregatorRawUpdate, ta: TimeAggregator, derivator: RawAggregator)
                                        (implicit ec: ExecutionContextExecutor)
{
   def aggregations: Future[List[AggUpdate]] = derivator.aggregate(ru, ta)
}

/**
 * A collection of graphes / flows that could be useful in various contexts.
 */
object Utils extends AggUpdateJsonSupport with ObservationJsonSupport
{
   def ru2Agg(derivators: List[RawAggregator], timeAggregator: Option[TimeAggregator])
                     (implicit builder: GraphDSL.Builder[NotUsed], ec: ExecutionContextExecutor): FlowShape[TimeAggregatorRawUpdate, AggUpdate] =
   {
      timeAggregator match {
         case None => Utils.deadEnd[TimeAggregatorRawUpdate, AggUpdate]
         case Some(ta) => {
            builder.add(
               Flow[TimeAggregatorRawUpdate].mapConcat(
                  ru => derivators.map(derivator => RawAggregationTrigger(ru, ta, derivator))
               ).mapAsyncUnordered(4)(trigger => trigger.aggregations)
                  .mapConcat(x => x)
                  .buffer(500, OverflowStrategy.backpressure)
            )
         }
      }
   }

   def obsToKafkaMsg(implicit builder: GraphDSL.Builder[NotUsed]): FlowShape[Observation, ProducerRecord[String, String]] =
      builder.add(
         Flow[Observation]
            .map(obs => new ProducerRecord[String, String]("observations", obs.toJson.compactPrint))
      )

   /**
    * Creates a flow shape disconnecting its input from its output,
    * in such a way inputs are ignored and nothing is actually produced.
    * @param builder
    * @tparam I
    * @tparam O
    * @return
    */
   def deadEnd[I, O](implicit builder: GraphDSL.Builder[NotUsed]): FlowShape[I, O] =
   {
      val sink = builder.add(Sink.ignore)
      val source = builder.add(Source.empty[O])

      FlowShape(sink.in, source.out)
   }
}