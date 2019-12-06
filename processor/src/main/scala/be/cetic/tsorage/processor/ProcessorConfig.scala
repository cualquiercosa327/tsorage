package be.cetic.tsorage.processor

import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._

/**
  * A representation of the Processor configuration.
 *
 * Path of configuration file is tsorage/ingestion/src/main/resources/tsorage.conf.
 *
 * To configure Cassandra host, you have to set the TSORAGE_CASSANDRA_HOST environment variable (defaults to localhost).
 * To configure list of addresses of Kafka brokers in a bootstrap Kafka cluster, you have to set the
 * TSORAGE_KAFKA_BROKER_ADDRESSES environment variable (defaults to [localhost:9092]).
 *
  */
object ProcessorConfig extends LazyLogging
{
   private val cassandraHost = System.getenv().getOrDefault("TSORAGE_CASSANDRA_HOST", "localhost")
   private val kafkaBrokerAddresses = System.getenv().getOrDefault("TSORAGE_KAFKA_BROKER_ADDRESSES", "[localhost:9092]")
   private val kafkaBrokerAddressList = kafkaBrokerAddresses.substring(1, kafkaBrokerAddresses.length - 1)
        .split(",").toList // Convert a String (of the form "[x,y,z]") to a List.

   val conf: Config = ConfigFactory.load("tsorage.conf")
     .withValue("cassandra.host", ConfigValueFactory.fromAnyRef(s"$cassandraHost"))
     .withValue("kafka.bootstrap", ConfigValueFactory.fromIterable(kafkaBrokerAddressList.asJava))

   val forbiddenTagnames = conf.getStringList("forbidden_tags")

   /**
     * Remove any tag having a forbidden name.
     * @param message   A message
     * @return The message, without any tag with forbidden name.
     */
   def dropBadTags(message: Message): Message = {
      val (rejected, accepted) = message.tagset.span(entry => forbiddenTagnames contains entry._1)

      if(!rejected.isEmpty)
         logger.info(s"Rejected tags due to forbidden tagname: ${rejected}")

      Message(
         message.metric,
         accepted,
         message.`type`,
         message.values
      )
   }

   /**
     * Generate the list of workers that will temporally aggregate incoming values.
     * The generation is proposed according to the "aggregators" property of the configuration file.
     * @return The sequence of aggregators to call when a value enters the processor.
     */
   def aggregators(): List[TimeAggregator] =
   {
      val names = conf.getStringList("aggregators").asScala

      val aggs = names.scanLeft(("raw", Option.empty[TimeAggregator])){ case ((previousName, previousAgg), name) => (name, Some(TimeAggregator(name, previousName))) }
      aggs.toList.flatMap(agg => agg._2)
   }

   val rawKS: String = conf.getString("cassandra.keyspaces.raw")
   val aggKS: String = conf.getString("cassandra.keyspaces.aggregated")
}
