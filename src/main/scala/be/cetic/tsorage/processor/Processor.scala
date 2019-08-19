package be.cetic.tsorage.processor

import java.time.{LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture

import be.cetic.tsorage.processor.sharder.Sharder
import com.datastax.driver.core.{BatchStatement, BoundStatement, ConsistencyLevel, DataType, Session}
import com.typesafe.scalalogging.LazyLogging
import com.datastax.driver.core.schemabuilder.SchemaBuilder._
import com.datastax.driver.core.querybuilder.QueryBuilder._

import scala.jdk.CollectionConverters._
import scala.collection.parallel.CollectionConverters._

import scala.util.Try


/**
  * This entity is responsible of processing a message.
  *
  * 1. Notify the system that a new tagset is used. (blocking)
  * 2. Batch-insert of the raw values, shard by shard. (blocking)
  * 3. Flatten of the raw values, for rollup
  * 4. Buffering of the flatten raw values
  * 5. Processing each time series, by (metric, tagset)
  */
case class Processor(session: Session, sharder: Sharder) extends LazyLogging
{
   /**
     * Maximum number of inserts in a single batch.
     * This value is specified by technical limitations of Cassandra / its driver, which is 65'536 statements at the time
     * of writting this class. https://docs.datastax.com/en/developer/java-driver/4.1/manual/core/statements/batch/
     *
     * Conveniently, that's enough for submitting the values of an entire minute, at 60.000 Hz (1 value / millisec)
     */
   private final val BATCH_INSERT_SIZE = 65000;

   /**
     * Notifies the system that some tag names has been ingested.
     * The system is responsible of integrating this, e.g. for querying purpose.
     *
     * If the tag names have already been notified, this action does nothing.
     * Consequently, this method is idempotent.
     *
     * @param tagnames the keys of the tagset to notify.
     */
   def notify(tagnames: Set[String]) =
   {
      logger.info(s"Notifying tagnames $tagnames")

      /*
       * TODO : Make it asynchronous ?
       *        Anyway, the whole foreach must be blocking
       */
      tagnames.foreach(tagname => {

         val alterRaw = alterTable("tsorage_raw", "numeric")
            .addColumn("tagname").`type`(DataType.text())

         val alterAgg = alterTable("tsorage_agg", "numeric")
            .addColumn(tagname).`type`(DataType.text())

         Try(session.execute(alterRaw))
         Try(session.execute(alterAgg))
      })
   }

   /**
     * Submits to the TSDB the raw values represented in a message.
     *
     * Submissions are asynchronous, but the whole method is blocking.
     * Because of the data model used to store these values, this operation is idempotent.
     *
     * @param message   The message containing the values to be submitted.
     */
   def submitRawObservations(message: FloatMessage): Unit =
   {
      logger.info(s"Submitting ${message.values.size} raw values to (${message.metric}, ${message.tagset})")
      val sharded: Map[String, List[(LocalDateTime, Float)]] = message.values.groupBy(observation => sharder.shard(observation._1))
      val javatags : java.util.Map[String, String] = message.tagset.asJava

      val baseInsertQuery = insertInto("tsorage_raw", "numeric")
         .value("metric", message.metric)
         .values(message.tagset.keys.toList.asJava, message.tagset.asInstanceOf[Map[String, AnyRef]].values.toList.asJava)

      def processBatch(shardId: String, batchContent: List[(LocalDateTime, Float)]) =
      {
         /*
          * TODO : Prepared statements are supposed to be much more efficient,
          *   but binding them blocks the workflow. Investigate + fix it.
          *
          *   https://medium.com/netflix-techblog/astyanax-update-c936487bb0c0
          */

         val shardedQuery = baseInsertQuery.value("shard", shardId)

         val statements = batchContent.map(content => shardedQuery
            .value("datetime", content._1.toInstant(ZoneOffset.UTC).toEpochMilli)
            .value("value", content._2)
         ).asJava

         val batch = new BatchStatement(BatchStatement.Type.UNLOGGED)
            .addAll(statements)
            .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

         session.execute(batch)
      }

      def processShard(shardId: String, shardContent: List[(LocalDateTime, Float)]) =
      {
         shardContent
            .sliding(BATCH_INSERT_SIZE, BATCH_INSERT_SIZE)
            .foreach(batch => processBatch(shardId, batch))
      }

      sharded.foreach(shard => processBatch(shard._1, shard._2))
   }

   def process(message: FloatMessage) =
   {
      notify(message.tagset.keySet)
      submitRawObservations(message)
      message.values.map(v => (message.metric, message.tagset, v._1))
   }
}
