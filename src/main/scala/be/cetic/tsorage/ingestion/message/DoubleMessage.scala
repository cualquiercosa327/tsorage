package be.cetic.tsorage.ingestion.message

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.TimeZone

/**
 * A message, provided by an external client.
 */
case class DoubleMessage(
                          metric: String,
                          points: List[(Double, Double)], // The first term of the pair is the Unix timestamp, in second (with optional decimal)
                          `type`: Option[String],
                          interval: Option[Long],
                          host: Option[String],
                          tags: List[String] // format: "key:value"
                       )
{
   def prepared() = {

      val preparedTags = tags.map(tag => tag.split(":", 2)).filter(tag => tag.size == 2).map(tag => tag(0) -> tag(1)).toMap

      val preparedTagsWithType = `type` match {
         case None => preparedTags
         case Some(t) => preparedTags + ("type" -> t)
      }

      val preparedTagsWithInterval = interval match {
         case None => preparedTagsWithType
         case Some(i) => preparedTagsWithType + ("interval" -> i.toString)
      }

      val preparedTagsWithHost = host match {
         case None => preparedTagsWithInterval
         case Some(h) => preparedTagsWithInterval + ("host" -> h)
      }

      PreparedDoubleMessage(
         metric,
         preparedTagsWithHost,
         "double",
         points.map(point => (
            LocalDateTime.ofEpochSecond(point._1.toLong, 0, ZoneOffset.UTC),
            point._2
         ))
      )
   }
}
