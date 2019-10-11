package be.cetic.tsorage.processor

import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._

/**
  * A representation of the Processor configuration
  */
object ProcessorConfig extends LazyLogging
{
   val conf = ConfigFactory.load("tsorage.conf")

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
