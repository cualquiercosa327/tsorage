package be.cetic.tsorage.collector.modbus

import be.cetic.tsorage.common.messaging.Message
import be.cetic.tsorage.collector.modbus._
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import collection.JavaConverters._

/**
 * An entity that converts Modbus responses to Messages,
 * according to the requests associated with the responses and the data cartography.
 *
 * @param indexedReadCoilsRequests The association of a (unitId, transactionId) to a Modbus request, for ReadCoils requests.
 * @param indexedReadDiscreteInputRequests  The association of a (unitId, transactionId) to a Modbus request, for ReadDiscreteInput requests.
 * @param indexedReadHoldingRegisterRequests  The association of a (unitId, transactionId) to a Modbus request, for ReadHoldingRegister requests.
 * @param indexedReadInputRegisterRequests  The association of a (unitId, transactionId) to a Modbus request, for InputRegister requests.
*/
case class MessageFactory(
                            indexedReadCoilsRequests: Map[(Int, Int), ReadCoils],
                            indexedReadDiscreteInputRequests: Map[(Int, Int), ReadDiscreteInput],
                            indexedReadHoldingRegisterRequests: Map[(Int, Int), ReadHoldingRegister],
                            indexedReadInputRegisterRequests: Map[(Int, Int), ReadInputRegister],
                            deviceConfig: Config
                         ) extends LazyLogging
{
   def responseToMessages(response: ModbusResponse): List[Message] = response match
   {
      case error: ModbusErrorResponse => {
         logger.warn(s"Modbus Error Response: ${error}")
         List()
      }

      case valid: ModbusValidResponse => {
         val request = valid match {
            case rc: ReadCoilsValidResponse =>              indexedReadCoilsRequests((rc.unitId, rc.transactionId))
            case rdi: ReadDiscreteInputValidResponse =>     indexedReadDiscreteInputRequests((rdi.unitId, rdi.transactionId))
            case rhr: ReadHoldingRegisterValidResponse =>   indexedReadHoldingRegisterRequests((rhr.unitId, rhr.transactionId))
            case rir: ReadInputRegisterValidResponse =>     indexedReadInputRegisterRequests((rir.unitId, rir.transactionId))
         }

         val offset = request.registerNumber
         val length = request.registerCount

         val extract_name = valid match {
            case _: ReadCoilsValidResponse =>              "output_coils"
            case _: ReadDiscreteInputValidResponse =>     "input_contacts"
            case _: ReadHoldingRegisterValidResponse =>   "holding_registers"
            case _: ReadInputRegisterValidResponse =>     "input_registers"
         }

         val extracts = if(deviceConfig.hasPath(extract_name)) deviceConfig.getConfigList(extract_name).asScala.toList
                        else List.empty

         def matchingUnitId(extract: Config): Boolean = deviceConfig.getInt("unit_id") == request.unitId

         def matchingByteRange(extract: Config): Boolean = {
            val extractLength = extract.getInt("address")
            (offset <= extractLength) && (offset + length >= extractLength + typeToRegisterNumber(extract.getString("type")))
         }

         val relevant_extracts = extracts.filter(extract => {
            matchingUnitId(extract) &&
            matchingByteRange(extract)
         })

         relevant_extracts.map(extract => {

            val `type` = extract.getString("type")
            val bytes = valid
               .data
               .drop(extract.getInt("address") - offset)
               .take(2 * typeToRegisterNumber(`type`))      // 2 bytes per register

            bytesToMessage(bytes, extract)
         })
      }
   }
}
