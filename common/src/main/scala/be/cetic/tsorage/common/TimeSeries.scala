package be.cetic.tsorage.common

/**
 * A representation of a time series.
 */
case class TimeSeries(metric: String, tagset: Map[String, String])
