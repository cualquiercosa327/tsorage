package be.cetic.tsorage.processor

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.cassandra.CassandraBatchSettings
import akka.stream.scaladsl.{Sink, Source}
import be.cetic.tsorage.common.json.MessageJsonSupport
import be.cetic.tsorage.processor.aggregator.time.{DayAggregator, HourAggregator, MinuteAggregator}
import be.cetic.tsorage.processor.flow.GlobalProcessingGraphFactory
import be.cetic.tsorage.processor.source.RandomMessageIterator
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.{Logger, LoggerFactory}
import spray.json._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.FiniteDuration

object Main extends LazyLogging with MessageJsonSupport
{
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  def main(args: Array[String]): Unit =
  {
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

/*
    def inboundMessagesConnector(): Source[Message, _] = Consumer
       .plainSource(consumerSettings, Subscriptions.topics(conf.getString("kafka.topic")))
       .map(record => Message.messageFormat.read(record.value().parseJson))
*/
     def inboundMessagesConnector() = RandomMessageIterator.source()

    val settings: CassandraBatchSettings = CassandraBatchSettings(1000, FiniteDuration(20, TimeUnit.SECONDS))

    val aggregators = ProcessorConfig.aggregators()

    val processorGraph = GlobalProcessingGraphFactory.createGraph(aggregators)

    inboundMessagesConnector()
       .via(processorGraph)
       .runWith(Sink.ignore)
  }
}
