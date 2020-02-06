package be.cetic.tsorage.collector.modbus

import java.time.{LocalDateTime, ZoneId}

import be.cetic.tsorage.common.messaging.Message
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

/**
 * An entity that converts Modbus responses to Messages,
 * according to the requests associated with the responses and the data cartography.
 *
 * @param requestDict The association of a (unitId, transactionId) to a Modbus request, for every supported type of requests.
*/
case class MessageFactory(requestDict: Map[ModbusFunction, Map[(Int, Int), ModbusRequest]],
                          extractDict: Map[ModbusFunction, List[Extract]],
                          unitId: Int
                         ) extends LazyLogging
{
   def responseToMessages(response: ModbusResponse): List[Message] = response match
   {
      case error: ModbusErrorResponse => {
         logger.warn(s"Modbus Error Response: ${error}")
         List()
      }

      case valid: ModbusValidResponse => {

         val function = valid.function

         val request =  requestDict(function)((valid.unitId, valid.transactionId))
         val extracts = extractDict(function)
         val offset = request.registerNumber

         val relevant_extracts = extracts.filter(extract => extract.matches(request, valid))

         relevant_extracts.map(extract => {
            val bytes = valid
               .data
               .drop(extract.address - offset)
               .take(2 * typeToRegisterNumber(extract.`type`))      // 2 bytes per register

            extract.bytesToMessage(bytes, LocalDateTime.now(ZoneId.of("UTC")))
         })
      }
   }
}
