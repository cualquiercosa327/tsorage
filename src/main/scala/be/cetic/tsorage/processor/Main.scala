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
import com.datastax.oss.driver.api.core.cql._


import scala.util.Random
import java.time.format.DateTimeFormatter



object Main
{
   val shardFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

   def inboundMessagesConnector(): Source[FloatMessage, NotUsed] = {
      val prepared = LazyList.from(0).map(d => FloatMessage("my sensor", Map(), List((LocalDateTime.of(2019, 7, 24, Random.nextInt(24), Random.nextInt(60), Random.nextInt(60), 0), Random.nextFloat()))))
      Source(prepared)
   }


   /**
     * Notifies the system that some tags has been ingested.
     * The system is responsible of integrating this, for querying purpose.
     *
     * If the tags have already been notified, there is normally nothing to do.
     * So a Redis could be used as cache for avoiding any database query.
     * @param tagnames   The tags keys to notify
     */
   def notify(tagnames: Set[String]) =
   {
      /**
        * 1. Check the tagnames that must be added to Cassandra
        * 2. Add the appropriate columns in a try/catch
        */
   }

   /**
     * Converts a datetime into the shard it belongs to
     * @param dt  The datetime to convert
     * @return The shard corresponding to dt.
     */
   def shard(dt : LocalDateTime) = dt.format(shardFormatter)




   def main(args: Array[String]): Unit =
   {
/*
      val session = Cluster.builder
         .addContactPoint("127.0.0.1")
         .withPort(9042)
         .withoutJMXReporting()
         .build
         .connect()

      implicit val system = ActorSystem()
      implicit val mat = ActorMaterializer()
*/

      val groupedSize = 100

      val timeAggregators = List(MinuteAggregator, HourAggregator, DayAggregator)

      // val processor = Processor(session, "tsorage")
/*
      // Getting a stream of messages from an imaginary external system as a Source, and bufferize them
      val messages: Source[FloatMessage, NotUsed] = inboundMessagesConnector().throttle(50, FiniteDuration(5, TimeUnit.SECONDS))

      val batches = messages.groupedWithin(groupedSize, duration.FiniteDuration(5, TimeUnit.MINUTES))


      val minuteDates = batches.map(batch => batch.map(message => message.sensor -> message.values.map(_._1)))

      minuteDates.runWith(Sink.foreach(println))

      //grouped.map(x => x.size).runWith(Sink.foreach(println))

 */

      print(shard(LocalDateTime.now()))
   }
}
