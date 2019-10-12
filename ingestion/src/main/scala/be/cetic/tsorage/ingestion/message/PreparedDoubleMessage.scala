package be.cetic.tsorage.ingestion.message

import java.time.LocalDateTime

/**
 * A message, structured like an internal TSorage message.
 */
case class PreparedDoubleMessage(
   metric: String,
   tagset: Map[String, String],
   `type`: String,
   values: List[(LocalDateTime, Double)]
)