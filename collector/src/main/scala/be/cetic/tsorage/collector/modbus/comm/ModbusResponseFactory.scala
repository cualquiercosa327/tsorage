package be.cetic.tsorage.collector.modbus.comm

import akka.util.ByteString
import be.cetic.tsorage.collector.modbus.data.{ByteDataConverter, ShortDataConverter}
import be.cetic.tsorage.collector.modbus._
import be.cetic.tsorage.collector.modbus.comm.rtu.{ModbusRTUResponse, ReadCoilsErrorRTUResponse, ReadCoilsValidRTUResponse, ReadDiscreteInputErrorRTUResponse, ReadDiscreteInputValidRTUResponse, ReadHoldingRegisterErrorRTUResponse, ReadHoldingRegisterValidRTUResponse, ReadInputRegisterErrorRTUResponse, ReadInputRegisterValidRTUResponse}
import be.cetic.tsorage.collector.modbus.comm.tcp.{ModbusTCPResponse, ReadCoilsErrorTCPResponse, ReadCoilsValidTCPResponse, ReadDiscreteInputErrorTCPResponse, ReadDiscreteInputValidTCPResponse, ReadHoldingRegisterErrorTCPResponse, ReadHoldingRegisterValidTCPResponse, ReadInputRegisterErrorTCPResponse, ReadInputRegisterValidTCPResponse}

/**
 * An entity mapping Modbus response frames to Modbus Responses.
 */
object ModbusResponseFactory
{
   final private def data2Exception(data: Array[Byte]): Option[ModbusException] = data.headOption.map(ModbusException(_))

   def fromTCPByteString(payload: ByteString): ModbusTCPResponse = {
      val bytes = payload.toArray

      val transactionId: Int = ShortDataConverter.asUnsignedShort(bytes.slice(0, 2), false).toInt
      val unitId: Int = ByteDataConverter.asUnsignedByte(bytes.slice(6, 7), false).toInt
      val fc: Int = ByteDataConverter.asUnsignedByte(bytes.slice(7, 8), false).toInt

      lazy val fullData = bytes.drop(8)
      lazy val messageData = bytes.drop(9)

      if(fc >= 0x80)
      {
         val exception = data2Exception(bytes.drop(2).take(1))

         ModbusFunction(fc - 0x80) match {
            case ReadCoils => new ReadCoilsErrorTCPResponse(transactionId, unitId, exception)
            case ReadDiscreteInput => new ReadDiscreteInputErrorTCPResponse(transactionId, unitId, exception)
            case ReadHoldingRegister => new ReadHoldingRegisterErrorTCPResponse(transactionId, unitId, exception)
            case ReadInputRegister => new ReadInputRegisterErrorTCPResponse(transactionId, unitId, exception)
         }
      }
      else
      {
         ModbusFunction(fc) match {
            case ReadCoils => new ReadCoilsValidTCPResponse(transactionId, unitId, messageData)
            case ReadDiscreteInput => new ReadDiscreteInputValidTCPResponse(transactionId, unitId, messageData)
            case ReadHoldingRegister => new ReadHoldingRegisterValidTCPResponse(transactionId, unitId, messageData)
            case ReadInputRegister => new ReadInputRegisterValidTCPResponse(transactionId, unitId, messageData)
         }
      }
   }

   def fromRTUByteString(payload: ByteString): ModbusRTUResponse = {
      val bytes = payload.toArray

      val unitId: Int = ByteDataConverter.asUnsignedByte(bytes.slice(0, 1), false).toInt
      val fc: Int = ByteDataConverter.asUnsignedByte(bytes.slice(1, 2), false).toInt

      lazy val messageData = bytes.drop(3).dropRight(2)

      if(fc >= 0x80)
      {
         val exception = data2Exception(bytes.drop(2).take(1))

         ModbusFunction(fc - 0x80) match {
            case ReadCoils => new ReadCoilsErrorRTUResponse(unitId, exception)
            case ReadDiscreteInput => new ReadDiscreteInputErrorRTUResponse(unitId, exception)
            case ReadHoldingRegister => new ReadHoldingRegisterErrorRTUResponse(unitId, exception)
            case ReadInputRegister => new ReadInputRegisterErrorRTUResponse(unitId, exception)
         }
      }
      else
      {
         ModbusFunction(fc) match {
            case ReadCoils => new ReadCoilsValidRTUResponse(unitId, messageData)
            case ReadDiscreteInput => new ReadDiscreteInputValidRTUResponse(unitId, messageData)
            case ReadHoldingRegister => new ReadHoldingRegisterValidRTUResponse(unitId, messageData)
            case ReadInputRegister => new ReadInputRegisterValidRTUResponse(unitId, messageData)
         }
      }
   }
}
