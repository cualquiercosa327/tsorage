package be.cetic.tsorage.collector.modbus

import java.time.LocalDateTime

import be.cetic.tsorage.common.messaging.Message
import com.typesafe.config.Config
import spray.json.{JsBoolean, JsNumber, JsString}

/**
 * A representation of the extraction of a value, from a Modbus device.
 */
case class Extract(
                     address: Int,
                     `type`: String,
                     metric: String,
                     tagset: Map[String, String],
                     highByteFirst: Boolean,
                     highWordFirst: Boolean,
                     rank: Option[Int],
                     dictionary: Map[Int, String]
                  )
{
   /**
    * Determines whether the provided response complies with this extract.
    * Basically, register position and count are compared with those expected by the extract.
    * @param response   A modbus response.
    * @return           true if the response corresponds to this extract; false otherwise.
    */
   def matches(request: ModbusRequest, response: ModbusValidResponse): Boolean =
   {
      val matchingUnitId: Boolean = response.unitId == request.unitId
      val matchingByteRange: Boolean = {
         val requestOffset = request.registerNumber
         val responseLength = response.data.length
         val extractLength = 2 * typeToRegisterNumber(`type`)  // Two bytes per register

         (requestOffset <= address) && (requestOffset + responseLength >= address + extractLength )
      }

      matchingUnitId && matchingByteRange
   }

   /**
    * Converts a payload into a message
    * @param bytes         The representation of a value.
    * @param datetime      The datetime to associated with the message.
    * @return              The message, containing the extracted value and the specified datetime.
    */
   def bytesToMessage(bytes: Array[Byte], datetime: LocalDateTime): Message =
   {
      assert(bytes.length == 2 * typeToRegisterNumber(`type`))   // 2 bytes per register

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



object Extract
{
   def apply(extractConfig: Config): Extract =
   {
      val address = extractConfig.getInt("address")

      val tagset = if(extractConfig.hasPath("tagset")) extractConfig
         .getObject("tagset")
         .keySet.toArray
         .map(key => key.toString -> extractConfig.getConfig("tagset").getString(key.toString) )
         .toMap
                   else Map.empty[String, String]

      val highByteFirst = {
         if(extractConfig.hasPath("byte_order")) Some(extractConfig.getString("byte_order"))
         else None
         }.map(order => order match {
         case "HIGH_BYTE_FIRST" => true
         case _ => false
      }).getOrElse(true)

      val highWordFirst = {
         if(extractConfig.hasPath("word_order")) Some(extractConfig.getString("word_order"))
         else None
         }.map(order => order match {
         case "HIGH_WORD_FIRST" => true
         case _ => false
      }).getOrElse(true)

      val rank = if(extractConfig.hasPath("rank")) Some(extractConfig.getInt("rank"))
                 else None

      val dictionary: Map[Int, String] = if(extractConfig.hasPath("values")) extractConfig
         .getObject("values")
         .keySet.toArray
         .map(key => key.toString.toInt -> extractConfig.getConfig("values").getString(key.toString) )
         .toMap
                                         else Map.empty


      Extract(
         address,
         extractConfig.getString("type"),
         extractConfig.getString("metric"),
         tagset,
         highByteFirst,
         highWordFirst,
         rank,
         dictionary
      )
   }
}