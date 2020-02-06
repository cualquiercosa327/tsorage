package be.cetic.tsorage.collector.modbus

import akka.util.ByteString


/**
 * An entity mapping Modbus response frames to Modbus Responses.
 */
object ModbusResponseFactory
{
   final private def data2Exception(data: Array[Byte]): Option[ModbusException] = data.headOption.map(ModbusException(_))

   def fromByteString(payload: ByteString): ModbusResponse = {
      val bytes = payload.toArray

      val transactionId: Int = ShortDataConverter.asUnsignedShort(bytes.slice(0, 2), false)
      val unitId: Int = ByteDataConverter.asUnsignedByte(bytes.slice(6, 7), false)
      val fc: Int = ByteDataConverter.asUnsignedByte(bytes.slice(7, 8), false)

      lazy val fullData = bytes.drop(8)
      lazy val messageData = bytes.drop(9)

      if(fc >= 0x80)
      {
         ModbusFunction(fc - 0x80) match {
            case ReadCoils => new ReadCoilsValidResponse(transactionId, unitId, messageData)
            case ReadDiscreteInput => new ReadDiscreteInputValidResponse(transactionId, unitId, messageData)
            case ReadHoldingRegister => new ReadHoldingRegisterValidResponse(transactionId, unitId, messageData)
            case ReadInputRegister => new ReadInputRegisterValidResponse(transactionId, unitId, messageData)
         }
      }
      else
      {
         ModbusFunction(fc) match {
            case ReadCoils => new ReadCoilsValidResponse(transactionId, unitId, messageData)
            case ReadDiscreteInput => new ReadDiscreteInputValidResponse(transactionId, unitId, messageData)
            case ReadHoldingRegister => new ReadHoldingRegisterValidResponse(transactionId, unitId, messageData)
            case ReadInputRegister => new ReadInputRegisterValidResponse(transactionId, unitId, messageData)
         }
      }
   }
}
