package be.cetic.tsorage.processor.update

import java.time.LocalDateTime

/**
 * An aggregated update, simplified in order to only show
 * informations relevant for a particular time aggregator.
 */
case class TimeAggregatorAggUpdate(
                                     metric: String,
                                     tagset: Map[String, String],
                                     shunk: LocalDateTime,
                                     `type`: String,
                                     interval: String,
                                     aggregation: String
                                  )
