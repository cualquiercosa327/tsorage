package be.cetic.tsorage.processor

import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.cassandra.CassandraBatchSettings
import akka.stream.scaladsl.{Sink, Source}
import akka.dispatch.ExecutionContexts
import be.cetic.tsorage.processor.aggregator.{DayAggregator, HourAggregator, MinuteAggregator}
import be.cetic.tsorage.processor.flow.TestFlow
import be.cetic.tsorage.processor.source.{KafkaConsumer, RandomMessageIterator}

import be.cetic.tsorage.processor.sharder.{DaySharder, MonthSharder}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.FiniteDuration


object Main extends LazyLogging with App
            with FloatMessageJsonSupport
{
  val shardFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

  def inboundMessagesConnector(): Source[FloatMessage, _] = new KafkaConsumer().deserializedSource[FloatMessage]

  val conf = ConfigFactory.load("tsorage.conf")
  val cassandraHost = conf.getString("cassandra.host")
  val cassandraPort = conf.getInt("cassandra.port")
  val root: Logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)

  val sharder = conf.getString("sharder") match {
    case "day" => DaySharder
    case _ => MonthSharder
  }

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val bufferGroupSize = 1000
  val bufferTime = FiniteDuration(1, TimeUnit.SECONDS)

  val settings: CassandraBatchSettings = CassandraBatchSettings(100, FiniteDuration(20, TimeUnit.SECONDS))

  val minuteAggregator = MinuteAggregator
  val hourAggregator = HourAggregator

  val aggregators = List(MinuteAggregator, HourAggregator, DayAggregator)

  val test = new TestFlow(inboundMessagesConnector(), List(minuteAggregator, hourAggregator), sharder)
     .flow
     .runWith(Sink.ignore)
}
