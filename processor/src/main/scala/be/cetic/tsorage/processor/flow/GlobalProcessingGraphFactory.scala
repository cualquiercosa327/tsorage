package be.cetic.tsorage.processor.flow

import java.time.LocalDateTime

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, MergePreferred, Sink}
import be.cetic.tsorage.common.json.{AggUpdateJsonSupport, ObservationJsonSupport}
import be.cetic.tsorage.common.{TimeSeries, messaging}
import be.cetic.tsorage.common.messaging.{AggUpdate, InformationVector, Message, Observation}
import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.producer.ProducerRecord
import GraphDSL.Implicits._
import be.cetic.tsorage.processor.aggregator.followup.AggAggregator
import be.cetic.tsorage.processor.aggregator.raw.{RawAggregator, SimpleRawAggregator}
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.update.{ShardedTagsetUpdate, TimeAggregatorRawUpdate}

import scala.concurrent.ExecutionContextExecutor
import spray.json._

/**
  * A factory for preparing the global processing graph
  */
object GlobalProcessingGraphFactory extends LazyLogging
   with ObservationJsonSupport
{
   private val tagUpdate = TagUpdateBlock(Cassandra.session)

   def createGraph(
                     timeAggregators: List[TimeAggregator],
                     obsObsDerivators: List[Observation => Seq[Observation]],
                     obsAggDerivators: List[Observation => Seq[AggUpdate]],
                     followUpDerivators: List[AggAggregator],
                     aggObsDerivators: List[AggUpdate => Seq[Observation]],
                     rawDerivators: List[RawAggregator],
                     kafkaSink: Sink[ProducerRecord[String, String], _]
                  )(implicit context: ExecutionContextExecutor) = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>

      val messageBlock = builder.add(MessageBlock.createGraph(timeAggregators.headOption, rawDerivators).async)
      val observationBlock = builder.add(ObservationBlock.createGraph(timeAggregators.headOption, List(), List()).async)
      val aggregationBlock = builder.add(AggregationBlock.createGraph(followUpDerivators, timeAggregators, List()).async)
      val tagUpdateBlock = builder.add(tagUpdate.createGraph.async)

      val tuCollector = builder.add(Merge[ShardedTagsetUpdate](3).async)
      val kafkaCollector = builder.add(Merge[ProducerRecord[String, String]](2).async)
      val auCollector = builder.add(MergePreferred[AggUpdate](1).async)

      messageBlock.tu ~> tuCollector
      messageBlock.au ~> auCollector.in(0)
      messageBlock.processedMessage ~> observationBlock.messageIn

      aggregationBlock.kafka ~> kafkaCollector
      aggregationBlock.tu ~> tuCollector
      aggregationBlock.processedAU ~> observationBlock.auIn

      observationBlock.tu ~> tuCollector
      observationBlock.au ~> auCollector.preferred
      observationBlock.kafka ~> kafkaCollector

      tuCollector ~> tagUpdateBlock
      kafkaCollector ~> kafkaSink
      auCollector ~> aggregationBlock.au

      SinkShape[Message](messageBlock.message)
   }
}
