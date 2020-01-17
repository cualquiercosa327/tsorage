package be.cetic.tsorage.processor

import java.time.LocalDateTime
import java.util.Date
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.{ActorMaterializer, SinkShape}
import akka.stream.alpakka.cassandra.CassandraBatchSettings
import akka.stream.scaladsl.{GraphDSL, Sink, Source}
import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.common.messaging.{AggUpdate, Message}
import be.cetic.tsorage.processor.flow.{GlobalProcessingGraphFactory, MessageBlock}
import be.cetic.tsorage.processor.source.RandomMessageIterator
import GraphDSL.Implicits._
import be.cetic.tsorage.common.TimeSeries
import be.cetic.tsorage.processor.aggregator.followup.{AggCountDerivator, HistoricalFollowUpAggregator}
import be.cetic.tsorage.processor.aggregator.raw.{HistoricalRawAggregator, RawAggregator, RawCountAggregator}
import be.cetic.tsorage.processor.aggregator.time.{MinuteAggregator, TimeAggregator}
import be.cetic.tsorage.processor.datatype.{DataTypeSupport, LongSupport}
import be.cetic.tsorage.processor.update.TimeAggregatorRawUpdate
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.slf4j.{Logger, LoggerFactory}
import be.cetic.tsorage.processor.aggregator.raw
import be.cetic.tsorage.processor.aggregator.followup

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.FiniteDuration
import spray.json._

object Processor extends LazyLogging with MessageJsonSupport
{
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  def main(args: Array[String]): Unit =
  {
    val conf = ProcessorConfig.conf
    val cassandraHost = conf.getString("cassandra.host")
    val cassandraPort = conf.getInt("cassandra.port")
    val root: Logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)

    val kafkaServers = conf.getStringList("kafka.bootstrap").asScala.mkString(";")

    val consumerSettings = ConsumerSettings(
      system,
      new StringDeserializer,
      new StringDeserializer
    )
       .withBootstrapServers(kafkaServers)
       .withGroupId(conf.getString("kafka.group"))
    //  .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
       .withBootstrapServers(kafkaServers)

    val kafkaSink = Producer.plainSink(producerSettings)

    def inboundMessagesConnector(): Source[Message, _] = Consumer
       .plainSource(consumerSettings, Subscriptions.topics(conf.getString("kafka.topic")))
       .map(record => record.value.parseJson.convertTo[Message])

//     def inboundMessagesConnector() = RandomMessageIterator.source()

    val aggregators = ProcessorConfig.aggregators()

    val rawDerivators: List[RawAggregator] = List(
      HistoricalRawAggregator(List(RawCountAggregator) ++ raw.tdouble.simpleRawDerivators)
    )

    val followUpDerivators = List(
      HistoricalFollowUpAggregator(List(AggCountDerivator) ++ followup.tdouble.simpleFollowUpDerivators)
    )

    val processorGraph = GlobalProcessingGraphFactory.createGraph(
      aggregators,
      List(),
      List(),
      followUpDerivators,
      List(),
      rawDerivators,
      kafkaSink)

    inboundMessagesConnector().to(processorGraph).run()
  }
}
