package be.cetic.tsorage.collector.modbus

import java.time.{LocalDateTime, ZoneId}

import be.cetic.tsorage.common.messaging.Message
import be.cetic.tsorage.collector.modbus._
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import collection.JavaConverters._

/**
 * An entity that converts Modbus responses to Messages,
 * according to the requests associated with the responses and the data cartography.
 *
 * @param indexedRequests The association of a (unitId, transactionId) to a Modbus request, for every supported type of requests.
*/
case class MessageFactory( indexedRequests: Map[String, Map[(Int, Int), ModbusRequest]],
                           deviceConfig: Config,
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

         val extract_name = valid match {
            case _: ReadCoilsValidResponse =>             "output_coils"
            case _: ReadDiscreteInputValidResponse =>     "input_contacts"
            case _: ReadHoldingRegisterValidResponse =>   "holding_registers"
            case _: ReadInputRegisterValidResponse =>     "input_registers"
         }

         val request = indexedRequests(extract_name)((valid.unitId, valid.transactionId))

         val offset = request.registerNumber
         val length = request.registerCount

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

            val preparedExtract = Extract(extract)

            val bytes = valid
               .data
               .drop(extract.getInt("address") - offset)
               .take(2 * typeToRegisterNumber(preparedExtract.`type`))      // 2 bytes per register

            preparedExtract.bytesToMessage(bytes, LocalDateTime.now(ZoneId.of("UTC")))
         })
      }
   }
}
