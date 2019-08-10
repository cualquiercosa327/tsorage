package be.cetic.tsorage.processor

import java.net.{Inet4Address, InetSocketAddress}
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



object Main extends LazyLogging
{
   val shardFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

   def inboundMessagesConnector(): Source[FloatMessage, NotUsed] = {
      val prepared = LazyList.from(0).map(d => FloatMessage("my sensor", Map(), List((LocalDateTime.of(2019, 7, 24, Random.nextInt(24), Random.nextInt(60), Random.nextInt(60), 0), Random.nextFloat()))))
      Source(prepared)
   }


   def main(args: Array[String]): Unit =
   {
      /*
      val session = Cluster.builder
         .addContactPoint("127.0.0.1")
         .withPort(9042)
         .withoutJMXReporting()
         .build
         .connect()

       */

      val session = CqlSession.builder()
         //.addContactPoint(new InetSocketAddress("127.0.0.1", 9042))
         //.withLocalDatacenter("DC1")
         .build()


/*
      implicit val system = ActorSystem()
      implicit val mat = ActorMaterializer()
*/

      val groupedSize = 100

      val timeAggregators = List(MinuteAggregator, HourAggregator, DayAggregator)

      val processor = Processor(session, MonthSharder)

      processor.process(FloatMessage(
         "my sensor",
         Map("owner2" -> "mg", "quality" -> "miam"),
         List(
            (LocalDateTime.now(), 42.0F),
            (LocalDateTime.now(), 1337.0F),
         )
      ))
/*
      // Getting a stream of messages from an imaginary external system as a Source, and bufferize them
      val messages: Source[FloatMessage, NotUsed] = inboundMessagesConnector().throttle(50, FiniteDuration(5, TimeUnit.SECONDS))

      val batches = messages.groupedWithin(groupedSize, duration.FiniteDuration(5, TimeUnit.MINUTES))


      val minuteDates = batches.map(batch => batch.map(message => message.sensor -> message.values.map(_._1)))

      minuteDates.runWith(Sink.foreach(println))

      //grouped.map(x => x.size).runWith(Sink.foreach(println))

 */


   }
}
