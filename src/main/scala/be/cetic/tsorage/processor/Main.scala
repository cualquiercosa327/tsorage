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
import com.datastax.driver.core.querybuilder.Insert
import com.datastax.driver.core.querybuilder.QueryBuilder.{bindMarker, insertInto}
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.shaded.guava.common.cache.{CacheBuilder, CacheLoader}
import com.typesafe.scalalogging.LazyLogging
import org.slf4j.{Logger, LoggerFactory}
import org.slf4j.event.Level

import scala.concurrent.duration.FiniteDuration
import collection.JavaConverters._


object RandomMessageIterator extends Iterator[FloatMessage]
{
   private val tagNames = List("status", "owner", "biz", "test O'test", "~bar")
   private val tagValues = List("hello", "there", "Clara O'toll", "~foo", "Ahaha \" !")

   override def hasNext() = true

   def randomTags(): FloatMessage = FloatMessage(
      "my sensor",
      Map(
         tagNames(Random.nextInt(tagNames.size)) -> tagValues(Random.nextInt(tagValues.size)),
         tagNames(Random.nextInt(tagNames.size)) -> tagValues(Random.nextInt(tagValues.size)),
         tagNames(Random.nextInt(tagNames.size)) -> tagValues(Random.nextInt(tagValues.size))),
      List((LocalDateTime.now, Random.nextFloat())))



   def simpleNext = FloatMessage(
      "my sensor",
      Map(
         "status" -> "ok",
         "owner" -> "myself"
      ),
      List((LocalDateTime.now, Random.nextFloat())))

   override def next(): FloatMessage = simpleNext
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

       /**
        * Binds a prepared insert statement for a raw observation.
        */
      val bindRawInsert: (FloatObservation, PreparedStatement) => BoundStatement = (observation: FloatObservation, prepared: PreparedStatement) => {
         val baseBound = prepared.bind()
            .setString("metric", observation.metric)
            .setString("shard", sharder.shard(observation.datetime))
            .setTimestamp("datetime", java.sql.Timestamp.valueOf(observation.datetime))
            .setFloat("value", observation.value)

         val folder: (BoundStatement, (String, String)) => BoundStatement =  (prev: BoundStatement, tag: (String, String)) => prev.setString(tag._1, tag._2)

         observation.tagset.foldLeft(baseBound)(folder)
      }

      val getRawInsertPreparedStatement : FloatObservation => PreparedStatement = {

         val cache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build(
               new CacheLoader[Set[String], PreparedStatement] {
                  def load(tags: Set[String]): PreparedStatement = {
                     val tagnames = tags.toList
                     val tagMarkers = tags.map(tag => bindMarker(tag)).toList

                     val baseStatement = insertInto("tsorage_raw", "numeric")
                        .value("metric", bindMarker("metric"))
                        .value("shard", bindMarker("shard"))
                        .value("datetime", bindMarker("datetime"))
                        .value("value", bindMarker("value"))

                     val folder: (Insert, String) => Insert = (base, tagname) => base.value(tagname, bindMarker(tagname))
                     val finalStatement = tags.foldLeft(baseStatement)(folder)

                     session.prepare(finalStatement)
                  }
               }
            )

         val f: FloatObservation => PreparedStatement = observation => {
            cache.get(observation.tagset.keySet)
         }

         f
      }


      val fanOutObservations: FloatMessage => List[FloatObservation] = { message => message.values.map(v => FloatObservation(message.metric, message.tagset, v._1, v._2)) }

      /**
        * A function ensuring all tagnames contained in a message
        * are prepared in the Cassandra database. The retrieved object
        * is the message itself, and the tagname management is a side effect.
        */
      val notifyTagnames: (FloatMessage => FloatMessage) = {

         var cache: Set[String] = Set()

         val f: FloatMessage => FloatMessage = msg => {
            val recentTags = msg.tagset.keySet.diff(cache)
            cache = cache ++ recentTags
            logger.info(s"Alter table with columns ${recentTags}")

            recentTags.map(tag => s"""ALTER TABLE tsorage_raw.numeric ADD "${tag.replace("\"", "\"\"")}" text;""")
                  .foreach(t => Try(session.execute(t)))

            recentTags.map(tag => s"""ALTER TABLE tsorage_agg.numeric ADD "${tag.replace("\"", "\"\"")}" text;""")
               .foreach(t => Try(session.execute(t)))

            msg
         }

         f
      }

      val bufferGroupSize = 1000
      val bufferTime = FiniteDuration(1, TimeUnit.SECONDS)

      val timeAggregators = List(MinuteAggregator, HourAggregator, DayAggregator)

      // Getting a stream of messages from an imaginary external system as a Source, and bufferize them
      val messages: Source[FloatMessage, NotUsed] = inboundMessagesConnector()

      val settings: CassandraBatchSettings = CassandraBatchSettings(100, FiniteDuration(20, TimeUnit.SECONDS))

      val rawFlow = AltCassandraFlow.createWithPassThrough[FloatObservation](16,
         getRawInsertPreparedStatement,
         bindRawInsert)(session)

      val test = messages
         .map(notifyTagnames)
         .mapConcat(fanOutObservations)
         .via(rawFlow)
         .runWith(Sink.ignore)
   }
}
