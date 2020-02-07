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
                     `type`: ModbusDataType,
                     metric: String,
                     tagset: Map[String, String]
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
         val extractLength = `type`.byteCount

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
      Message(
         metric,
         tagset,
         `type`.msgCode,
         List((datetime, `type`.bytesToJson(bytes)))
      )
   }
}


object Extract
{
   def apply(extractConfig: Config): Extract =
   {
      val address = Integer.decode(extractConfig.getString("address"))

      val tagset = if(extractConfig.hasPath("tagset")) extractConfig
         .getObject("tagset")
         .keySet.toArray
         .map(key => key.toString -> extractConfig.getConfig("tagset").getString(key.toString) )
         .toMap
                   else Map.empty[String, String]

      val `type` = ModbusDataType(extractConfig)

      Extract(
         address,
         `type`,
         extractConfig.getString("metric"),
         tagset
      )
   }
}