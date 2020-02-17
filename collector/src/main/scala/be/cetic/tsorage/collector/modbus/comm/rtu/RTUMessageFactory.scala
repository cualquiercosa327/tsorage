package be.cetic.tsorage.collector.modbus.comm.rtu

import java.time.{LocalDateTime, ZoneId}

import be.cetic.tsorage.collector.modbus.comm.ModbusRequest
import be.cetic.tsorage.collector.modbus.{Extract, ModbusFunction}
import be.cetic.tsorage.common.messaging.Message
import com.typesafe.scalalogging.LazyLogging

/**
 * An entity that converts Modbus responses to Messages,
 * according to the requests associated with the responses and the data cartography.
 *
 * @param extractDict The list of extracts associated with a function.
 */
case class RTUMessageFactory(extractDict: Map[ModbusFunction, List[Extract]]) extends LazyLogging
{
   def responseToMessages(currentRequest: ModbusRequest, response: ModbusRTUResponse): List[Message] = response match
   {
      case error: ModbusErrorRTUResponse =>
      {
         logger.warn(s"Modbus Error Response: ${error}")
         List()
      }

      case invalid: ModbusInvalidCRCResponse =>
      {
         logger.warn(s"Modbus CRC error: ${invalid}")
         List()
      }

      case valid: ModbusValidRTUResponse =>
      {
         val function = valid.function

         val sameUnitId = currentRequest.unitId == valid.unitId
         val sameFunction = currentRequest.function == function
         val matching = sameUnitId && sameFunction

         if(matching)
         {
            val extracts = extractDict(function)
            val offset = currentRequest.registerNumber

            val relevant_extracts = extracts.filter(extract => extract.matches(currentRequest, valid))

            relevant_extracts.map(extract =>
            {
               val bytes = valid
                  .data
                  .drop(extract.address - offset)
                  .take(extract.`type`.byteCount)

               extract.bytesToMessage(bytes, LocalDateTime.now(ZoneId.of("UTC")))
            })
         }
         else
         {
            logger.warn(s"Unmatching Modbus response: request was ${currentRequest}, response is ${valid}")
            List()
         }
      }
   }
}
