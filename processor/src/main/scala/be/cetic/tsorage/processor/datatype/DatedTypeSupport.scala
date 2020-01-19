package be.cetic.tsorage.processor.datatype
import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneId, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.Date

import com.datastax.driver.core.UDTValue
import spray.json.{JsObject, JsString, JsValue}

/**
 * A data type support for types associated with a date.
 *
 * @support The support of the underlying data type
 */
case class DatedTypeSupport[T](support: DataTypeSupport[T]) extends DataTypeSupport[DatedType[T]]
{
   override val colname: String = s"value_date_${support.`type`}"

   override val `type`: String = s"date_${support.`type`}"

   override def asJson(value: DatedType[T]): JsValue = new JsObject(Map(
      "datetime" -> JsString(DatedTypeSupport.format.format(value.datetime)),
      "value" -> support.asJson(value.value)
   ))

   override def fromJson(value: JsValue): DatedType[T] = value match
   {
      case JsObject(fields) =>
      {
         val date = fields("datetime") match
         {
            case JsString(x) => LocalDateTime.parse(x, DatedTypeSupport.format)
         }

         val payload = support.fromJson(fields("value"))

         DatedType[T](date, payload)
      }
   }

   override def fromUDTValue(value: UDTValue): DatedType[T] =
   {
         DatedType(
            DataTypeSupport.date2ldt(value.getTimestamp("datetime")),
            support.fromUDTValue(value.getUDTValue("value"))
         )
   }

   /**
    * Converts a value into a Cassandra UDT Value
    *
    * @param value The value to convert
    * @return The UDTValue representing the value
    */
   override def asRawUdtValue(value: DatedType[T]): UDTValue = rawUDTType
      .newValue()
      .setTimestamp("datetime", Timestamp.from(value.datetime.atOffset(ZoneOffset.UTC).toInstant))
      .setUDTValue("value", support.asRawUdtValue(value.value))

   /**
    * Converts a value into a Cassandra UDT value.
    *
    * @param value The value to convert
    * @return The UDTValue representing the value
    */
   override def asAggUdtValue(value: DatedType[T]): UDTValue = aggUDTType
      .newValue()
      .setTimestamp("datetime", Timestamp.from(value.datetime.atOffset(ZoneOffset.UTC).toInstant))
      .setUDTValue("value", support.asAggUdtValue(value.value))
}

object DatedTypeSupport
{
   val format = DateTimeFormatter.ISO_DATE_TIME

   val availableSupports: Map[String, DatedTypeSupport[_]] = DataTypeSupport.availableSupports
      .values
      .map(support => DatedTypeSupport(support))
      .map(support => support.`type` -> support).toMap

   def inferSupport(`type`: String): DatedTypeSupport[_] = availableSupports(`type`)
}
