package be.cetic.tsorage.collector.modbus.comm.tcp

import be.cetic.tsorage.collector.modbus._

sealed abstract class ModbusErrorTCPResponse(
                                            transactionId: Int,
                                            unitId: Int, fc: ModbusFunction,
                                            val exception: Option[ModbusException]
                                         ) extends ModbusTCPResponse(transactionId, unitId, fc)
{
   override def toString = s"${this.getClass.getSimpleName}($transactionId, $unitId, $fc, ${exception})"
}

class ReadCoilsErrorTCPResponse(
                               transactionId: Int,
                               unitId: Int,
                               exception: Option[ModbusException]
                            ) extends ModbusErrorTCPResponse(transactionId, unitId, ReadCoils, exception)

class ReadDiscreteInputErrorTCPResponse(
                                       transactionId: Int,
                                       unitId: Int,
                                       exception: Option[ModbusException]
                                    ) extends ModbusErrorTCPResponse(transactionId, unitId, ReadDiscreteInput, exception)

class ReadHoldingRegisterErrorTCPResponse(
                                         transactionId: Int,
                                         unitId: Int,
                                         exception: Option[ModbusException]
                                      ) extends ModbusErrorTCPResponse(transactionId, unitId, ReadHoldingRegister, exception)

class ReadInputRegisterErrorTCPResponse(
                                       transactionId: Int,
                                       unitId: Int,
                                       exception: Option[ModbusException]
                                    ) extends ModbusErrorTCPResponse(transactionId, unitId, ReadInputRegister, exception)