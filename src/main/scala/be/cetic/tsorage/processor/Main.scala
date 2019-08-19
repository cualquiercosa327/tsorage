package be.cetic.tsorage.processor

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Sink, Source}
import be.cetic.tsorage.processor.aggregator.{DayAggregator, HourAggregator, MinuteAggregator}
import com.datastax.driver.core.Cluster

import scala.util.Random
import java.time.format.DateTimeFormatter

import be.cetic.tsorage.processor.sharder.MonthSharder
import com.datastax.oss.driver.api.core.CqlSession
import com.typesafe.scalalogging.LazyLogging
import org.slf4j.{Logger, LoggerFactory}
import org.slf4j.event.Level

import scala.concurrent.duration.FiniteDuration



object Main extends LazyLogging
{
   val shardFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

   def inboundMessagesConnector(): Source[FloatMessage, NotUsed] = {
      val prepared = LazyList.from(0).map(d => FloatMessage("my sensor", Map("owner2" -> "Clara O'Tool"), List((LocalDateTime.now, Random.nextFloat()))))
      Source(prepared)
   }


   def main(args: Array[String]): Unit =
   {
      val root: Logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
      root

      val session = Cluster.builder
         .addContactPoint("127.0.0.1")
         .withPort(9042)
         .withoutJMXReporting()
         .build
         .connect()

      implicit val system = ActorSystem()
      implicit val mat = ActorMaterializer()

      val bufferGroupSize = 10
      val bufferTime = FiniteDuration(10, TimeUnit.SECONDS)

      val timeAggregators = List(MinuteAggregator, HourAggregator, DayAggregator)
      val sharder = MonthSharder

      val processor = Processor(session, sharder)


      // Getting a stream of messages from an imaginary external system as a Source, and bufferize them
      val messages: Source[FloatMessage, NotUsed] = inboundMessagesConnector()
         //.throttle(10, FiniteDuration(5, TimeUnit.SECONDS))

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

      byDay.runWith(Sink.ignore)

      /*
            val batches = messages.groupedWithin(groupedSize, duration.FiniteDuration(5, TimeUnit.MINUTES))

            val minuteDates = batches.map(batch => batch.map(message => message.sensor -> message.values.map(_._1)))

            minuteDates.runWith(Sink.foreach(println))

            //grouped.map(x => x.size).runWith(Sink.foreach(println))

       */
   }
}
