package be.cetic.tsorage.processor.flow

import akka.stream.scaladsl.Flow
import be.cetic.tsorage.common.messaging.{AggUpdate, Message, Observation}
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.ProcessorConfig
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContextExecutor, Future}


object CassandraWriter extends LazyLogging
{
   private def writeMessageAsync(msg: Message)(implicit context: ExecutionContextExecutor) = {
      logger.info(s"Cassandra is logging message ${msg}")
      Cassandra.submitMessageAsync(msg)
   }

   private def writeAggUpdate(update: AggUpdate)(implicit context: ExecutionContextExecutor) = {
      logger.info(s"Cassandra is logging agg update ${update}")
      Cassandra.submitAggUpdateAsync(update)
   }

   private def writeObservationAsync(obs: Observation)(implicit context: ExecutionContextExecutor): Future[Observation] = {
      Cassandra.submitObservationAsync(obs)
   }

   def createWriteAggFlow(implicit context: ExecutionContextExecutor) =
      Flow[AggUpdate].mapAsyncUnordered(ProcessorConfig.conf.getInt("parallelism"))(update => writeAggUpdate(update))

   def createWriteMsgFlow(implicit context: ExecutionContextExecutor) =
      Flow[Message].mapAsyncUnordered(ProcessorConfig.conf.getInt("parallelism"))(message => writeMessageAsync(message))

   def createWriteObsFlow(implicit context: ExecutionContextExecutor) =
      Flow[Observation].mapAsyncUnordered(ProcessorConfig.conf.getInt("parallelism"))(obs => writeObservationAsync(obs))
}
