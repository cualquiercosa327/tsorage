package be.cetic.tsorage.collector.modbus.comm.tcp

import java.time.{LocalDateTime, ZoneId}

import be.cetic.tsorage.collector.modbus.comm.ModbusRequest
import be.cetic.tsorage.collector.modbus.{Extract, ModbusFunction}
import be.cetic.tsorage.common.messaging.Message
import com.typesafe.scalalogging.LazyLogging

/**
 * An entity that converts Modbus responses to Messages,
 * according to the requests associated with the responses and the data cartography.
 *
 * @param requestDict The association of a (unitId, transactionId) to a Modbus request, for every supported type of requests.
*/
case class TCPMessageFactory(requestDict: Map[ModbusFunction, Map[(Int, Int), ModbusRequest]],
                             extractDict: Map[ModbusFunction, List[Extract]]
                            ) extends LazyLogging
{
   def responseToMessages(response: ModbusTCPResponse): List[Message] = response match
   {
      case error: ModbusErrorTCPResponse => {
         logger.warn(s"Modbus Error Response: ${error}")
         List()
      }

      case valid: ModbusValidTCPResponse => {

         val function = valid.function

         val request =  requestDict(function)((valid.unitId, valid.transactionId))
         val extracts = extractDict(function)
         val offset = request.registerNumber

         val relevant_extracts = extracts.filter(extract => extract.matches(request, valid))

         relevant_extracts.map(extract => {
            val bytes = valid
               .data
               .drop(extract.address - offset)
               .take(extract.`type`.byteCount)

            extract.bytesToMessage(bytes, LocalDateTime.now(ZoneId.of("UTC")))
         })
      }
   }
}
