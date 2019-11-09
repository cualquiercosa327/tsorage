package be.cetic.tsorage.processor.datatype
import be.cetic.tsorage.processor.aggregator.data.{CountAggregation, DataAggregation}
import be.cetic.tsorage.processor.update.AggUpdate
import com.datastax.driver.core.UDTValue
import spray.json.{JsNumber, JsValue}

/**
  * A support object for the long data type.
  */
object LongSupport extends DataTypeSupport[Long]
{
   override val colname = "value_long"
   override def `type` = "tlong"

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

   /**
     * Converts a value into a Cassandra UDT Value
     *
     * @param value The value to convert
     * @return The UDTValue representing the value
     */
   override def asRawUdtValue(value: Long): UDTValue =
      rawUDTType
         .newValue()
         .setLong("value", value)

   override def asAggUdtValue(value: Long): UDTValue =
      aggUDTType
         .newValue()
         .setLong("value", value)

   override def fromUDTValue(value: UDTValue): Long = value.getLong("value")
}
