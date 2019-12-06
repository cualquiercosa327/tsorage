package be.cetic.tsorage.processor.update

import java.time.LocalDateTime

import be.cetic.tsorage.common.TimeSeries

/**
 * An aggregated update, simplified in order to only show
 * informations relevant for a particular time aggregator.
 */
case class TimeAggregatorAggUpdate(
                                     ts: TimeSeries,
                                     shunk: LocalDateTime,
                                     `type`: String,
                                     interval: String,
                                     aggregation: String
                                  )
