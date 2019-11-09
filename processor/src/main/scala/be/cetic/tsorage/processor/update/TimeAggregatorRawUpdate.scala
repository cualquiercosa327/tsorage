package be.cetic.tsorage.processor.update

import java.time.LocalDateTime

import be.cetic.tsorage.common.TimeSeries

/**
 * A raw update, simplified in order to only show
 * informations relevant for a particular time aggregator.
 */
case class TimeAggregatorRawUpdate(ts: TimeSeries, shunk: LocalDateTime, `type`: String)
