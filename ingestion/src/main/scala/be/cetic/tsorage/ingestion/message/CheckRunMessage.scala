package be.cetic.tsorage.ingestion.message

/**
 * A message sent during a check run.
 * Cf https://docs.datadoghq.com/api/?lang=bash#post-a-check-run
 */
case class CheckRunMessage(
   check: String,
   host_name: String,
   status: Option[Int],
   timestamp: Option[Long],
   message: Option[String],
   tags: Option[List[String]]
)
