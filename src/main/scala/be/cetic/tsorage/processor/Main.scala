package be.cetic.tsorage.processor

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import be.cetic.tsorage.processor.aggregator.{DayAggregator, HourAggregator, MinuteAggregator}
import com.datastax.driver.core.{BatchStatement, BoundStatement, Cluster, ConsistencyLevel, DataType, PreparedStatement, Session}
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder._

import scala.util.{Random, Try}
import java.time.format.DateTimeFormatter

import akka.dispatch.ExecutionContexts
import akka.stream.alpakka.cassandra.CassandraBatchSettings
import akka.stream.alpakka.cassandra.scaladsl.AltCassandraFlow
import be.cetic.tsorage.processor.sharder.MonthSharder
import be.cetic.tsorage.processor.source.{RandomMessageIterable, RandomMessageIterator}
import com.datastax.driver.core.querybuilder.Insert
import com.datastax.driver.core.querybuilder.QueryBuilder.{bindMarker, insertInto}
import com.datastax.oss.driver.api.core.CqlSession
import com.typesafe.config.ConfigFactory
import com.datastax.oss.driver.shaded.guava.common.cache.{CacheBuilder, CacheLoader}
import com.typesafe.scalalogging.LazyLogging
import org.slf4j.{Logger, LoggerFactory}
import org.slf4j.event.Level

import scala.concurrent.duration.FiniteDuration
import collection.JavaConverters._
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.flow.{CassandraFlow, ObservationFlow, TestFlow}


object Main extends LazyLogging with App {
  val shardFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

  def inboundMessagesConnector(): Source[FloatMessage, NotUsed] = RandomMessageIterator.source()

  val conf = ConfigFactory.load("storage.conf")
  val cassandraHost = conf.getString("cassandra.host")
  val cassandraPort = conf.getInt("cassandra.port")
  val root: Logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)


  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()


  val bufferGroupSize = 1000
  val bufferTime = FiniteDuration(1, TimeUnit.SECONDS)
  val timeAggregators = List(MinuteAggregator, HourAggregator, DayAggregator)

  // Getting a stream of messages from an imaginary external system as a Source, and bufferize them

  val settings: CassandraBatchSettings = CassandraBatchSettings(100, FiniteDuration(20, TimeUnit.SECONDS))


  val test = new TestFlow().flow
    .runWith(Sink.ignore)

}
