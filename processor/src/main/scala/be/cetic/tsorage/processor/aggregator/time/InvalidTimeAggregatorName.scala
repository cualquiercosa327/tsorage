package be.cetic.tsorage.processor.aggregator.time

/**
  * An exception raised when an invalid aggregator name is used.
  */
case class InvalidTimeAggregatorName(aggregatorName: String) extends IllegalArgumentException
