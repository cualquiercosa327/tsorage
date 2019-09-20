package be.cetic.tsorage.processor.aggregator.data

import java.util.Date

/**
  * An entity that aggregates groups of value of a particular type.
  *
  * @tparam T The type of aggregated data
  */
trait DataAggregator[T]
{
   /**
     * A function to perform on raw observations for counting them.
     */
   val rawCount: Iterator[(Date, T)] => Long = values => values.size

   /**
     * A function to perform on aggregated observations for counting them.
     */
   val aggCount: Iterator[(Date, Long)] => Long = counts => counts.map(_._2).sum

   /**
     * @return The aggregation functions to perform on raw values, associated with their respective names.
     */
   def rawAggregators: Map[String, Iterable[(Date, T)] => T]

   /**
     * @return The aggregation functions to perform on aggregated values, associated with their respective names.
     */
   def aggAggregators: Map[String, Iterable[(Date, T)] => T]

   /**
     * Temporal aggregation functions to perform on raw values, associated with their respective names.
     *
     * Contrary to ordinary aggregation functions, temporal aggregation functions are based on a "observed for the first/last time",
     * instead of the shunk time.
     */
   final val rawTempAggregators: Map[String, Iterable[(Date, T)] => (Date, T)] = Map(
      "first" -> {values => values.minBy(_._1)},
      "last" ->  {values => values.maxBy(_._1)}
   )

   /**
     * Temporal aggregation functions to perform on aggregated values, associated with their respective names.
     *
     * Contrary to ordinary aggregation functions, temporal aggregation functions are based on a "observed for the first/last time",
     * instead of the shunk time.
     */
   final val aggTempAggregators: Map[String, Iterable[(Date, T)] => (Date, T)] = rawTempAggregators
}
