package be.cetic.tsorage.hub.filter

/**
 * A query for time series, based on a metric, condition filters, and time range constraints.
 */
case class TimeSeriesQuery(metric: Option[String], filter: Filter, timeRange: Option[QueryDateRange])