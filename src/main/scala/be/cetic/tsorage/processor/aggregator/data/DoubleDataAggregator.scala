package be.cetic.tsorage.processor.aggregator.data
import java.util.Date

/**
  * A data aggregator for Double values.
  */
object DoubleDataAggregator extends DataAggregator[Double]
{
   /**
     * @return The aggregation functions to perform on raw values, associated with their respective names.
     */
   override def rawAggregators: Map[String, Iterable[(Date, Double)] => Double] = Map(
      "sum" ->   {values => values.map(_._2).sum},
      "s_sum" -> {values => values.map(x=> x._2*x._2).sum},
      "max" ->   {values => values.map(_._2).max},
      "min" ->   {values => values.map(_._2).min}
   )

   /**
     * @return The aggregation functions to perform on aggregated values, associated with their respective names.
     */
   override def aggAggregators: Map[String, Iterable[(Date, Double)] => Double] = Map(
      "sum" ->   {values => values.map(_._2).sum},
      "s_sum" -> {values => values.map(x=> x._2*x._2).sum},
      "max" ->   {values => values.map(_._2).max},
      "min" ->   {values => values.map(_._2).min}
   )
}
