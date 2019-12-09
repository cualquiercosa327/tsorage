package be.cetic.tsorage.processor.flow

import akka.stream.scaladsl.Flow
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.update.AggUpdate
import be.cetic.tsorage.processor.{Message, ProcessorConfig}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContextExecutor


object CassandraWriter extends LazyLogging
{
   private def writeMessageAsync(msg: Message)(implicit context: ExecutionContextExecutor) = {
      Cassandra.submitMessageAsync(msg)
   }

   private def writeAggUpdate(update: AggUpdate)(implicit context: ExecutionContextExecutor) = {
      //logger.info(s"Cassandra is logging agg update ${update}")
      Cassandra.submitAggUpdateAsync(update)
   }

   def createAggCassandraFlow(implicit context: ExecutionContextExecutor) =
      Flow[AggUpdate].mapAsyncUnordered(ProcessorConfig.conf.getInt("parallelism"))(update => writeAggUpdate(update))

   // def createMessageCassandraFlow = Flow.fromFunction(writeMessage)
   def createMessageCassandraFlow(implicit context: ExecutionContextExecutor) =
      Flow[Message].mapAsyncUnordered(ProcessorConfig.conf.getInt("parallelism"))(message => writeMessageAsync(message))
}
