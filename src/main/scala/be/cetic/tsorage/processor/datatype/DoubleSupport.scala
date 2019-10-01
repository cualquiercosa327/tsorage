package be.cetic.tsorage.processor.datatype

import be.cetic.tsorage.processor.AggUpdate
import be.cetic.tsorage.processor.aggregator.data.tdouble.{MaximumAggregation, MinimumAggregation, SumAggregation}
import be.cetic.tsorage.processor.aggregator.data.{CountAggregation, DataAggregation, FirstAggregation, LastAggregation}
import com.datastax.driver.core.{CodecRegistry, DataType, TypeCodec}
import spray.json.{JsNumber, JsValue}

/**
  * Support object for double data type.
  */
object DoubleSupport extends DataTypeSupport[Double]
{
   override val colname = "value_double_"
   override val codec = new CodecRegistry().codecFor(DataType.cdouble())
   override val `type` = "double"

   override def asJson(value: Double): JsValue = JsNumber(value)
   override def fromJson(value: JsValue): Double = value match {
      case JsNumber(x) => x.toDouble
      case _ => throw new IllegalArgumentException(s"Expected double; got ${value}")
   }

   override val rawAggregations: List[DataAggregation[Double, _]] = List(
      SumAggregation,
      MaximumAggregation,
      MinimumAggregation,
      CountAggregation[Double](this),
      FirstAggregation[Double](this, DateDoubleSupport),
      LastAggregation[Double](this, DateDoubleSupport)
   )

   /**
     * Finds the aggregation corresponding to a particular aggregated update.
     *
     * @param update The update from which an aggregation must be found.
     * @return The aggregation associated with the update.
     */
   override def findAggregation(update: AggUpdate): DataAggregation[_, Double] = update.aggregation match {
      case "sum" => SumAggregation
      case "max" => MaximumAggregation
      case "min" => MinimumAggregation
   }

   /**
     * Converts a value into a string representing this value as a Cassandra literal
     *
     * @param value The value to convert
     * @return The literal representation of the value for Cassandra.
     */
   override def asCassandraLiteral(value: Double): String = value.toString
}
