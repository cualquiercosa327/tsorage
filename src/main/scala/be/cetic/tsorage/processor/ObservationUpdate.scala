package be.cetic.tsorage.processor

import java.time.LocalDateTime

/**
  * Represents a change in an observation.
  * A change may be followed by other successive changes.
  */
case class ObservationUpdate[T](metric: String, tagset: Map[String, String], datetime: LocalDateTime, interval: String, values: Map[String, T])
