package be.cetic.tsorage.processor.update

import java.time.LocalDateTime

/**
 * A raw update, simplified in order to only show
 * informations relevant for a particular time aggregator.
 */
case class TimeAggregatorRawUpdate(metric: String, tagset: Map[String, String], shunk: LocalDateTime, `type`: String)
