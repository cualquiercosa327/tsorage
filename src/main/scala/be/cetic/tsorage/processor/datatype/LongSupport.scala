package be.cetic.tsorage.processor.datatype
import be.cetic.tsorage.processor.AggUpdate
import be.cetic.tsorage.processor.aggregator.data.{CountAggregation, DataAggregation}
import be.cetic.tsorage.processor.aggregator.data.tdouble.{MaximumAggregation, MinimumAggregation, SumAggregation}
import com.datastax.driver.core.{CodecRegistry, DataType, TypeCodec}
import spray.json.{DeserializationException, JsNumber, JsValue}

/**
  * A support for the long data type.
  */
object LongSupport extends DataTypeSupport[Long]
{
   override val colname = "value_long_"
   override val codec = new CodecRegistry().codecFor(DataType.bigint())
   override val `type` = "long"

   override def rawAggregations: List[DataAggregation[Long, _]] = List()

   override def asJson(value: Long): JsValue = JsNumber(value)
   override def fromJson(value: JsValue): Long = value match {
      case JsNumber(x) => x.toLong
      case _ => throw new IllegalArgumentException(s"Expected long; got ${value}")
   }

   /**
     * Finds the aggregation corresponding to a particular aggregated update.
     *
     * @param update The update from which an aggregation must be found.
     * @return The aggregation associated with the update.
     */
   override def findAggregation(update: AggUpdate): DataAggregation[_, Long] = update.aggregation match {
      case "count" => CountAggregation[Long](LongSupport)
   }
}
