package be.cetic.tsorage.processor.datatype

import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.Date

import be.cetic.tsorage.processor.{AggUpdate, ProcessorConfig}
import be.cetic.tsorage.processor.aggregator.data.{CountAggregation, DataAggregation, FirstAggregation, LastAggregation}
import be.cetic.tsorage.processor.aggregator.data.tdouble.{MaximumAggregation, MinimumAggregation, SumAggregation}
import be.cetic.tsorage.processor.datatype.DoubleSupport.{`type`, rawUDTType}
import com.datastax.driver.core.{CodecRegistry, DataType, ProtocolVersion, TupleType, UDTValue}
import com.datastax.oss.driver.api.core.`type`.{DataTypes, UserDefinedType}
import com.datastax.oss.driver.api.core.data.UdtValue
import com.datastax.oss.driver.internal.core.`type`.UserDefinedTypeBuilder
import spray.json.{JsNumber, JsObject, JsString, JsValue}


object DateDoubleSupport extends DataTypeSupport[(LocalDateTime, Double)]
{
   val format = DateTimeFormatter.ISO_DATE_TIME

   override val colname = "value_date_double_"
   override def `type` = "date_double"

   override def asJson(value: (LocalDateTime, Double)): JsValue = JsObject(
      "datetime" -> JsString(format.format(value._1)),
      "value" -> JsNumber(value._2)
   )
   override def fromJson(value: JsValue): (LocalDateTime, Double) = value match {
      case JsObject(fields) => {
         val date = fields("datetime") match {
            case JsString(x) => LocalDateTime.parse(x, format)
         }
         val value = fields("value") match {
            case JsNumber(x) => x.toDouble
         }

         (date, value)
      }
      case _ => throw new IllegalArgumentException(s"Expected date_double; got ${value}")
   }

   override val rawAggregations: List[DataAggregation[(LocalDateTime, Double), _]] = List( )

   /**
     * Finds the aggregation corresponding to a particular aggregated update.
     *
     * @param update The update from which an aggregation must be found.
     * @return The aggregation associated with the update.
     */
   override def findAggregation(update: AggUpdate): DataAggregation[Double, (LocalDateTime, Double)] = update.aggregation match {
      case "first" => FirstAggregation(DoubleSupport, this)
      case "last" => LastAggregation(DoubleSupport, this)
   }

   /**
     * Converts a value into a Cassandra UDT Value
     *
     * @param value The value to convert
     * @return The UDTValue representing the value
     */
   override def asRawUdtValue(value: (LocalDateTime, Double)): UDTValue =
      rawUDTType
         .newValue()
         .setTimestamp("datetime", Date.from( value._1.atZone( ZoneId.of("GMT")).toInstant()))
         .setDouble("value", value._2)


   override def asAggUdtValue(value: (LocalDateTime, Double)): UDTValue =
      aggUDTType
         .newValue()
         .setTimestamp("datetime", Date.from( value._1.atZone( ZoneId.of("GMT")).toInstant()))
         .setDouble("value", value._2)

   override def fromUDTValue(value: UDTValue): (LocalDateTime, Double) = (
       Instant
          .ofEpochMilli(value.getTimestamp("datetime").getTime())
          .atZone(ZoneId.of("GMT"))
          .toLocalDateTime(),
      value.getDouble("value")
   )
}
