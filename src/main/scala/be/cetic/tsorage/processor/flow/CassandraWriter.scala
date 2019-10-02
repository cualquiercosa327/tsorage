package be.cetic.tsorage.processor.flow

import akka.stream.scaladsl.Flow
import be.cetic.tsorage.processor.database.Cassandra
import be.cetic.tsorage.processor.{AggUpdate, RawUpdate}
import com.typesafe.scalalogging.LazyLogging


object CassandraWriter extends LazyLogging
{
   private def writeRawUpdate(update: RawUpdate) = {
      logger.info(s"Cassandra is logging raw update ${update}")
      Cassandra.submitRawUpdate(update)
      update
   }

   private def writeAggUpdate(update: AggUpdate) = {
      logger.info(s"Cassandra is logging agg update ${update}")
      Cassandra.submitAggUpdate(update)
      update
   }

   def createRawCassandraFlow = Flow.fromFunction(writeRawUpdate)
   def createAggCassandraFlow = Flow.fromFunction(writeAggUpdate)
}
