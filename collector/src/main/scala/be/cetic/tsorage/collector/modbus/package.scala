package be.cetic.tsorage.collector

import java.time.{LocalDateTime, ZoneId}

import be.cetic.tsorage.common.messaging.Message
import com.typesafe.config.Config
import spray.json.{JsBoolean, JsNumber, JsString}

package object modbus
{
   private val charRegex = """char(\d+)""".r

   def typeToRegisterNumber(`type`: String): Int =
   {
      `type` match {
         case "bool16" => 1

         case "uint16" => 1
         case "sint16" => 1

         case "uint32" => 2
         case "sint32" => 2

         case "sfloat32" => 2
         case "enum16" => 1

         case charRegex(length) => (length.toInt / 2) + 1
      }
   }

   /**
    * Converts a payload represented as a byte array, to a message,
    * according to an extract configuration.
    * @param bytes      The payload.
    * @param extract    The extract configuration mapping the bytes to a message.
    * @return           The message corresponding to bytes, according to extract.
    */
   def bytesToMessage(bytes: Array[Byte], extract: Config): Message =
   {
      val `type` = extract.getString("type")
      assert(bytes.length == 2 * typeToRegisterNumber(`type`))   // 2 bytes per register

      val metric = extract.getString("metric")
      val datetime = LocalDateTime.now(ZoneId.of("GMT"))

      val tagset = if(extract.hasPath("tagset")) extract
         .getObject("tagset")
         .keySet.toArray
         .map(key => key.toString -> extract.getConfig("tagset").getString(key.toString) )
         .toMap
                   else Map.empty[String, String]


      val highByteFirst = {
         if(extract.hasPath("byte_order")) Some(extract.getString("byte_order"))
         else None
      }.map(order => order match {
         case "HIGH_BYTE_FIRST" => true
         case _ => false
      }).getOrElse(true)

      val highWordFirst = {
         if(extract.hasPath("word_order")) Some(extract.getString("word_order"))
         else None
         }.map(order => order match {
         case "HIGH_WORD_FIRST" => true
         case _ => false
      }).getOrElse(true)

      lazy val ordered = DataConverter.orderNormalization(bytes, !highByteFirst, !highWordFirst)

      `type` match {
         case "bool16" => {
            val value = ShortDataConverter.asUnsignedShort(bytes, !highByteFirst) != 0
            Message(
               metric,
               tagset,
               "tbool",
               List((datetime, JsBoolean(value)))
            )
         }

         case "uint16" => {
            val value = ShortDataConverter.asUnsignedShort(bytes, !highByteFirst)
            Message(
               metric,
               tagset,
               "tlong",
               List((datetime, JsNumber(value)))
            )
         }

         case "sint16" => {
            val value = ShortDataConverter.asSignedShort(bytes, !highByteFirst)
            Message(
               metric,
               tagset,
               "tlong",
               List((datetime, JsNumber(value)))
            )
         }

         case "uint32" => {
            val value = IntDataConverter.asUnsignedInt(ordered)
            Message(
               metric,
               tagset,
               "tlong",
               List((datetime, JsNumber(value)))
            )
         }

         case "sint32" => {
            val value = IntDataConverter.asSignedInt(ordered)
            Message(
               metric,
               tagset,
               "tlong",
               List((datetime, JsNumber(value)))
            )
         }

         case "sfloat32" => {
            val value = FloatDataConverter.asSignedFloat(ordered)
            Message(
               metric,
               tagset,
               "tdouble",
               List((datetime, JsNumber(value)))
            )
         }

         case "enum16" => {
            val numericValue = ShortDataConverter.asUnsignedShort(ordered, false)
            val dictionary: Map[Int, String] = if(extract.hasPath("values")) extract
                  .getObject("values")
                  .keySet.toArray
                  .map(key => key.toString.toInt -> extract.getConfig("values").getString(key.toString) )
                  .toMap
                                               else Map.empty

            Message(
               metric,
               tagset,
               "ttext",
               List((datetime, JsString(dictionary.getOrElse(numericValue, numericValue.toString))))
            )
         }
      }
   }
}
