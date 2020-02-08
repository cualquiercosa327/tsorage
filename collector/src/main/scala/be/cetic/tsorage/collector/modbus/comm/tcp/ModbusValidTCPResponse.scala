package be.cetic.tsorage.collector.modbus.comm.tcp

import be.cetic.tsorage.collector.modbus._
import be.cetic.tsorage.collector.modbus.comm.ModbusValidResponse


sealed abstract class ModbusValidTCPResponse(
                                     transactionId: Int,
                                     unitId: Int,
                                     function: ModbusFunction,
                                     val data: Array[Byte]
                                  )
   extends ModbusTCPResponse(transactionId, unitId, function) with ModbusValidResponse
{
  override def toString = s"${this.getClass.getSimpleName}($transactionId, $unitId, ${data.map(byte => (byte & 0xFF).toHexString).mkString("[", ",", "]")})"
}

class ReadCoilsValidTCPResponse(
                               transactionId: Int,
                               unitId: Int,
                               data: Array[Byte]
                            ) extends ModbusValidTCPResponse(transactionId, unitId, ReadCoils, data)

class ReadDiscreteInputValidTCPResponse(
                                       transactionId: Int,
                                       unitId: Int,
                                       data: Array[Byte]
                                    ) extends ModbusValidTCPResponse(transactionId, unitId, ReadDiscreteInput, data)

class ReadHoldingRegisterValidTCPResponse(
                                         transactionId: Int,
                                         unitId: Int,
                                         data: Array[Byte]
                                      ) extends ModbusValidTCPResponse(transactionId, unitId, ReadHoldingRegister, data)


class ReadInputRegisterValidTCPResponse(
                                       transactionId: Int,
                                       unitId: Int,
                                       data: Array[Byte]
                                    ) extends ModbusValidTCPResponse(transactionId, unitId, ReadInputRegister, data)