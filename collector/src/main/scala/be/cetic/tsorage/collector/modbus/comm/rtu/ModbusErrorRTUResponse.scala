package be.cetic.tsorage.collector.modbus.comm.rtu

import be.cetic.tsorage.collector.modbus._

sealed abstract class ModbusErrorRTUResponse(
                                               unitId: Int, fc: ModbusFunction,
                                               val exception: Option[ModbusException]
                                            ) extends ModbusRTUResponse(unitId, fc)
{
   override def toString = s"${this.getClass.getSimpleName}($unitId, $fc, ${exception})"
}

class ReadCoilsErrorRTUResponse(
                                  unitId: Int,
                                  exception: Option[ModbusException]
                               ) extends ModbusErrorRTUResponse(unitId, ReadCoils, exception)

class ReadDiscreteInputErrorRTUResponse(
                                          unitId: Int,
                                          exception: Option[ModbusException]
                                       ) extends ModbusErrorRTUResponse(unitId, ReadDiscreteInput, exception)

class ReadHoldingRegisterErrorRTUResponse(
                                            unitId: Int,
                                            exception: Option[ModbusException]
                                         ) extends ModbusErrorRTUResponse(unitId, ReadHoldingRegister, exception)

class ReadInputRegisterErrorRTUResponse(
                                          unitId: Int,
                                          exception: Option[ModbusException]
                                       ) extends ModbusErrorRTUResponse(unitId, ReadInputRegister, exception)
