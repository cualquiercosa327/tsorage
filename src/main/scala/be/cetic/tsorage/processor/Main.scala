package be.cetic.tsorage.processor

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Sink, Source}
import be.cetic.tsorage.processor.aggregator.{DayAggregator, HourAggregator, MinuteAggregator}
import com.datastax.driver.core.{BatchStatement, BoundStatement, Cluster, ConsistencyLevel, PreparedStatement}

import scala.util.Random
import java.time.format.DateTimeFormatter

import akka.stream.alpakka.cassandra.CassandraBatchSettings
import akka.stream.alpakka.cassandra.scaladsl.{CassandraFlow, CassandraSink}
import be.cetic.tsorage.processor.sharder.MonthSharder
import com.datastax.driver.core.querybuilder.QueryBuilder.{bindMarker, insertInto}
import com.datastax.oss.driver.api.core.CqlSession
import com.typesafe.scalalogging.LazyLogging
import org.slf4j.{Logger, LoggerFactory}
import org.slf4j.event.Level

import scala.concurrent.duration.FiniteDuration
import collection.JavaConverters._


object RandomMessageIterator extends Iterator[FloatMessage]
{
   override def hasNext() = true
   override def next(): FloatMessage = FloatMessage("my sensor", Map("owner2" -> "Clara O'Tool"), List((LocalDateTime.now, Random.nextFloat())))
}

object RandomMessageIterable extends scala.collection.immutable.Iterable[FloatMessage]
{
   override def iterator: Iterator[FloatMessage] = RandomMessageIterator
}

object Main extends LazyLogging
{
   val shardFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

   def inboundMessagesConnector(): Source[FloatMessage, NotUsed] = Source(RandomMessageIterable)

   def main(args: Array[String]): Unit =
   {
      val session = Cluster.builder
         .addContactPoint("127.0.0.1")
         .withPort(9042)
         .withoutJMXReporting()
         .build
         .connect()

      implicit val system = ActorSystem()
      implicit val mat = ActorMaterializer()

      val sharder = MonthSharder

      val preparedRawInsert = session.prepare(
         insertInto("tsorage_raw", "numeric")
            .value("metric", bindMarker("metric"))
            .value("shard", bindMarker("shard"))
            .value("datetime", bindMarker("datetime"))
            .value("value", bindMarker("value"))
      )

      val bindRawInsert: (FloatObservation, PreparedStatement) => BoundStatement = (observation: FloatObservation, prepared: PreparedStatement) => {
         val x = observation.tagset.map(tag => tag._1)
         prepared.bind()
            .setString("metric", observation.metric)
            .setString("shard", sharder.shard(observation.datetime))
            .setTimestamp("datetime", java.sql.Timestamp.valueOf(observation.datetime))
            .setFloat("value", observation.value)
      }
            val fanOutObservations: FloatMessage => List[FloatObservation] = { message => message.values.map(v => FloatObservation(message.metric, message.tagset, v._1, v._2)) }

      val bufferGroupSize = 1000
      val bufferTime = FiniteDuration(1, TimeUnit.SECONDS)

      val timeAggregators = List(MinuteAggregator, HourAggregator, DayAggregator)

      // Getting a stream of messages from an imaginary external system as a Source, and bufferize them
      val messages: Source[FloatMessage, NotUsed] = inboundMessagesConnector()

      /**
        * A function ensuring all tagnames contained in a message
        * are prepared in the Cassandra database. The retrieved object
        * is the message itself, and the tagname management is a side effect.
        */
      val notifyTagnames: FloatMessage => FloatMessage = (message: FloatMessage) => message

      val settings: CassandraBatchSettings = CassandraBatchSettings(100, FiniteDuration(20, TimeUnit.SECONDS))

      val rawFlow = CassandraFlow.createWithPassThrough[FloatObservation](16,
         preparedRawInsert,
         bindRawInsert)(session)

      val test = messages
         .map(notifyTagnames)
         .mapConcat(fanOutObservations)
         .via(rawFlow)
         .runWith(Sink.ignore)

      /*
            val changes = messages
               .async
               .mapConcat(processor.process)

            val byMinute = changes
               .async
               .map(event => (event._1, event._2, MinuteAggregator.shunk(event._3)))
               .groupedWithin(bufferGroupSize, bufferTime)
               .mapConcat(events => events.toSet) // distinct
               .map(event => MinuteAggregator.updateShunk(session, event._1, event._2, event._3, sharder))

            val byHour = byMinute
               .async
               .map(event => (event._1, event._2, HourAggregator.shunk(event._3)))
               .groupedWithin(bufferGroupSize, bufferTime)
               .mapConcat(events => events.toSet) // distinct
               .map(event => HourAggregator.updateShunk(session, event._1, event._2, event._3, sharder))

            val byDay = byHour
               .async
               .map(event => (event._1, event._2, DayAggregator.shunk(event._3)))
               .groupedWithin(bufferGroupSize, bufferTime)
               .mapConcat(events => events.toSet) // distinct
               .map(event => DayAggregator.updateShunk(session, event._1, event._2, event._3, sharder))

      changes.runWith(Sink.ignore)
     */
      /*
            val batches = messages.groupedWithin(groupedSize, duration.FiniteDuration(5, TimeUnit.MINUTES))

            val minuteDates = batches.map(batch => batch.map(message => message.sensor -> message.values.map(_._1)))

            minuteDates.runWith(Sink.foreach(println))

            //grouped.map(x => x.size).runWith(Sink.foreach(println))

       */
   }
}
