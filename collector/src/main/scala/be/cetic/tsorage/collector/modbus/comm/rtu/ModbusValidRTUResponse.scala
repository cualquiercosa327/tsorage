package be.cetic.tsorage.collector.modbus.comm.rtu

import be.cetic.tsorage.collector.modbus._
import be.cetic.tsorage.collector.modbus.comm.ModbusValidResponse


sealed abstract class ModbusValidRTUResponse(
                                               unitId: Int,
                                               function: ModbusFunction,
                                               val data: Array[Byte]
                                            )
   extends ModbusRTUResponse(unitId, function) with ModbusValidResponse
{
   override def toString = s"${this.getClass.getSimpleName}($unitId, ${data.map(byte => (byte & 0xFF).toHexString).mkString("[", ",", "]")})"
}

class ReadCoilsValidRTUResponse(
                                  unitId: Int,
                                  data: Array[Byte]
                               ) extends ModbusValidRTUResponse(unitId, ReadCoils, data)

class ReadDiscreteInputValidRTUResponse(
                                          unitId: Int,
                                          data: Array[Byte]
                                       ) extends ModbusValidRTUResponse(unitId, ReadDiscreteInput, data)

class ReadHoldingRegisterValidRTUResponse(
                                            unitId: Int,
                                            data: Array[Byte]
                                         ) extends ModbusValidRTUResponse(unitId, ReadHoldingRegister, data)


class ReadInputRegisterValidRTUResponse(
                                          unitId: Int,
                                          data: Array[Byte]
                                       ) extends ModbusValidRTUResponse(unitId, ReadInputRegister, data)
