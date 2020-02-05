package be.cetic.tsorage.collector.modbus

import akka.util.ByteString


/**
 * An entity mapping Modbus response frames to Modbus Responses.
 */
object ModbusResponseFactory
{
   final private def data2Exception(data: Array[Byte]): Option[ModbusException] = data.headOption.map{
      case 0x01 => IllegalFunction
      case 0x02 => IllegalDataAddress
      case 0x03 => IllegalDataValue
      case 0x04 => SlaveDeviceFailure
      case 0x05 => Acknowledge
      case 0x06 => SlaveDeviceBusy
      case 0x07 => NegativeAck
      case 0x08 => MemoryParityError
      case 0x0A => GatewayPathProblem
      case 0x0B => GatewayTargetProblem
      case 0xFF => ExtendedException
   }

   def fromByteString(payload: ByteString): ModbusResponse = {
      val bytes = payload.toArray

      val transactionId: Int = ShortDataConverter.asUnsignedShort(bytes.slice(0, 2), false)
      val unitId: Int = ByteDataConverter.asUnsignedByte(bytes.slice(6, 7), false)
      val fc: Int = ByteDataConverter.asUnsignedByte(bytes.slice(7, 8), false)

      lazy val fullData = bytes.drop(8)
      lazy val messageData = bytes.drop(9)

      fc match {
         case 0x01 => new ReadCoilsValidResponse(transactionId, unitId, messageData)
         case 0x02 => new ReadDiscreteInputValidResponse(transactionId, unitId, messageData)
         case 0x03 => new ReadHoldingRegisterValidResponse(transactionId, unitId, messageData)
         case 0x04 => new ReadInputRegisterValidResponse(transactionId, unitId, messageData)
         case 0x81 => new ReadCoilsErrorResponse(transactionId, unitId, data2Exception(fullData))
         case 0x82 => new ReadDiscreteInputErrorResponse(transactionId, unitId, data2Exception(fullData))
         case 0x83 => new ReadHoldingRegisterErrorResponse(transactionId, unitId, data2Exception(fullData))
         case 0x84 => new ReadInputRegisterErrorResponse(transactionId, unitId, data2Exception(fullData))
      }
   }
}
