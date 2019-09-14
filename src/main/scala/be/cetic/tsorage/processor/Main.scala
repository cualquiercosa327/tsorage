package be.cetic.tsorage.processor

import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.cassandra.CassandraBatchSettings
import akka.stream.scaladsl.{Sink, Source}
import akka.dispatch.ExecutionContexts
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import be.cetic.tsorage.processor.aggregator.{DayAggregator, HourAggregator, MinuteAggregator}
import be.cetic.tsorage.processor.flow.{AggregationProcessingGraphFactory, GlobalProcessingGraphFactory, RawProcessingGraphFactory}
import be.cetic.tsorage.processor.source.{KafkaConsumer, RandomMessageIterator}
import be.cetic.tsorage.processor.sharder.{DaySharder, MonthSharder}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.{Logger, LoggerFactory}

import collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.FiniteDuration
import spray.json._


object Main extends LazyLogging with App
            with FloatMessageJsonSupport
{
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val conf = ConfigFactory.load("tsorage.conf")
  val cassandraHost = conf.getString("cassandra.host")
  val cassandraPort = conf.getInt("cassandra.port")
  val root: Logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)

  val consumerSettings = ConsumerSettings(
    system,
    new StringDeserializer,
    new StringDeserializer
  )
     .withBootstrapServers(conf.getStringList("kafka.bootstrap").asScala.mkString(";"))
     .withGroupId(conf.getString("kafka.group"))
   //  .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")


  def inboundMessagesConnector(): Source[FloatMessage, _] = Consumer
     .plainSource(consumerSettings, Subscriptions.topics(conf.getString("kafka.topic")))
     .map(record => record.value().parseJson.convertTo[FloatMessage])

  val bufferGroupSize = 1000
  val bufferTime = FiniteDuration(1, TimeUnit.SECONDS)

  val settings: CassandraBatchSettings = CassandraBatchSettings(100, FiniteDuration(20, TimeUnit.SECONDS))

  val minuteAggregator = new MinuteAggregator("raw")
  val hourAggregator = new HourAggregator(minuteAggregator.name)
  val dayAggregator = new DayAggregator(hourAggregator.name)

  val aggregators = List(minuteAggregator)

  //val rawProcessorGraph = RawProcessingGraphFactory.createGraph
  val processorGraph = GlobalProcessingGraphFactory.createGraph(aggregators)

  inboundMessagesConnector()
     .via(processorGraph)
     .runWith(Sink.ignore)
}
