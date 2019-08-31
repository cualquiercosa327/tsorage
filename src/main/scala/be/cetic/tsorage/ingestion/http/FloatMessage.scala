package be.cetic.tsorage.ingestion.http

import java.time.{Instant, LocalDateTime}
import java.util.TimeZone

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

/**
 * A message, provided by an external client.
 */
case class FloatMessage(
                          metric: String,
                          points: List[(Double, Float)], // The first term of the pair is the Unix timestamp, in second (with optional decima)
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

      PreparedFloatMessage(
         metric,
         preparedTagsWithHost,
         points.map(point => (LocalDateTime.ofInstant(Instant.ofEpochMilli((point._1 * 1000).toLong),
            TimeZone.getTimeZone("UTC").toZoneId())  , point._2))
      )
   }
}

/**
 * A message, structured like an internal TSorage message.
 */
case class PreparedFloatMessage(
   metric: String,
   tagset: Map[String, String],
   values: List[(LocalDateTime, Float)]
)

case class FloatBody(series: List[FloatMessage])

trait FloatMessageJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
   implicit val messageFormat = jsonFormat6(FloatMessage)
   implicit val bodyFormat = jsonFormat1(FloatBody)
}
