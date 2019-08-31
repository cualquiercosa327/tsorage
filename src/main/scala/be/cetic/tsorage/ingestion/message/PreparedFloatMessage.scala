package be.cetic.tsorage.ingestion.message

import java.time.LocalDateTime

import spray.json.DefaultJsonProtocol

/**
 * A message, structured like an internal TSorage message.
 */
case class PreparedFloatMessage(
   metric: String,
   tagset: Map[String, String],
   values: List[(LocalDateTime, Float)]
)

