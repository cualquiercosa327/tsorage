package be.cetic.tsorage.processor

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

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
   def dropBadTags(message: FloatMessage): FloatMessage = {
      val (rejected, accepted) = message.tagset.span(entry => forbiddenTagnames contains entry._1)

      if(!rejected.isEmpty)
         logger.info(s"Rejected tags due to forbidden tagname: ${rejected}")

      FloatMessage(
         message.metric,
         accepted,
         message.values
      )
   }
}
