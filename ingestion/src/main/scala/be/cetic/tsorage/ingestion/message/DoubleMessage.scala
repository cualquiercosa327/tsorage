package be.cetic.tsorage.ingestion.message

import java.time.{LocalDateTime, ZoneOffset}

import be.cetic.tsorage.common.messaging.{Message, User}
import be.cetic.tsorage.ingestion.IngestionConfig
import spray.json.JsNumber

/**
 * A message, provided by an external, Datadog-compliant client.
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
   /**
    * Transform a double message in order to comply with the internal message format.
    * @param user The user associated with the message.
    * @return  The message in the internal format
    */
   def prepare(user: User): Message = {

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

      val preparedTagsWithUser = IngestionConfig.conf.getBoolean("append_user") match {
         case true => preparedTagsWithHost + ("user_id" -> user.id.toString)
         case false => preparedTagsWithHost
      }

      Message(
         metric,
         preparedTagsWithUser,
         "tdouble",
         points.map(point => (
            LocalDateTime.ofEpochSecond(point._1.toLong, 0, ZoneOffset.UTC),
            JsNumber(point._2)
         ))
      )
   }
}
