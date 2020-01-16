package be.cetic.tsorage.processor.flow

import java.time.{LocalDateTime, ZoneId}

import akka.NotUsed
import akka.stream.{FlowShape, SinkShape, SourceShape, UniformFanOutShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source}
import GraphDSL.Implicits._
import be.cetic.tsorage.common.json.{AggUpdateJsonSupport, ObservationJsonSupport}
import be.cetic.tsorage.common.messaging.{AggUpdate, Observation}
import be.cetic.tsorage.processor.aggregator.raw.{RawAggregator, SimpleRawAggregator}
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import be.cetic.tsorage.processor.datatype.DataTypeSupport
import be.cetic.tsorage.processor.update.TimeAggregatorRawUpdate
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.ExecutionContextExecutor
import spray.json._

/**
 * A collection of graphes / flows that could be useful in various contexts.
 */
object Utils extends AggUpdateJsonSupport with ObservationJsonSupport
{
   def obsToKafkaMsg(implicit builder: GraphDSL.Builder[NotUsed]): FlowShape[Observation, ProducerRecord[String, String]] =
      builder.add(
         Flow[Observation]
            .map(obs => new ProducerRecord[String, String]("observations", obs.toJson.compactPrint))
      )

   /**
    * Create a flow converting raw updates into records. A record is a list of (dt, raw value) corresponding to
    * a given time range, for a particular time series. The time range is determined by the first aggregator.
    *
    * If there is no first aggregator, the flow is a dead end, that produces nothing.
    *
    * @param firstAggregator  The optional first aggregator used for converting raw values to aggregated values.
    * @param context
    * @param builder
    * @return
    */
   private def ruToRecords(firstAggregator: Option[TimeAggregator])
                          (implicit context: ExecutionContextExecutor, builder: GraphDSL.Builder[NotUsed])
    = firstAggregator match {
      case None => Utils.deadEnd

      case Some(aggregator) =>    {
         builder.add(
            Flow[TimeAggregatorRawUpdate].mapAsyncUnordered(8)(ru => {
               val support = DataTypeSupport.inferSupport(ru.`type`)
               val dateRange = aggregator.range(ru.shunk)
               support
                  .getRawValues(ru, dateRange._1, dateRange._2)
                  .map(l => (ru, l.toList))
            }).named("ruToRecords")
         )
      }
   }

   /**
    * Creates a flow converting records to aggregated updates. An aggregated update is a value, calculated as the
    * aggregation of some records belonging to a raw update.
    *
    * Aggregations are performed w.r.t the first temporal aggregation.
    *
    * @param derivators      The aggregation functions. Each function can produce zero, one or many aggregated values.
    *                   All these values constitute the flow output.
    * @param context
    * @param builder
    * @return
    */
   private def recordToAU(derivators: List[SimpleRawAggregator])
   (implicit context: ExecutionContextExecutor, builder: GraphDSL.Builder[NotUsed]):

   FlowShape[(TimeAggregatorRawUpdate, List[(LocalDateTime, JsValue)]), AggUpdate] =
   {
      builder.add(
         Flow[(TimeAggregatorRawUpdate, List[(LocalDateTime, JsValue)])].mapConcat(element => {
            derivators.flatMap(derivator => derivator.aggregate(element._1, element._2))
         } )
      )
   }

   /**
    * Converts a Raw Update (an event about a raw update), into aggregated updates.
    * @param firstAggregator  The time aggregation to process
    * @param derivators       The derivators, converting raw values into aggregated updates.
    * @param context
    * @return
    */
   def ruToFirstAgg(
                      firstAggregator: Option[TimeAggregator],
                      derivators: List[SimpleRawAggregator]
                   )(implicit context: ExecutionContextExecutor) =  GraphDSL.create()
   {
      implicit builder: GraphDSL.Builder[NotUsed] =>

      val records = ruToRecords(firstAggregator)
      val toAU = recordToAU(derivators)

      records ~> toAU

      FlowShape(records.in, toAU.out)
   }

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