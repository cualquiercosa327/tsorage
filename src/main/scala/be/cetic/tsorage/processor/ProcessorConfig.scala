package be.cetic.tsorage.processor

import be.cetic.tsorage.processor.aggregator.time.TimeAggregator
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

import collection.JavaConverters._

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
   def dropBadTags[T](message: Message[T]): Message[T] = {
      val (rejected, accepted) = message.tagset.span(entry => forbiddenTagnames contains entry._1)

      if(!rejected.isEmpty)
         logger.info(s"Rejected tags due to forbidden tagname: ${rejected}")

      Message[T](
         message.metric,
         accepted,
         message.values,
         message.support
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
}
