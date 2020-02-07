package be.cetic.tsorage.collector.modbus

import be.cetic.tsorage.common.messaging.Message

sealed abstract class ModbusErrorResponse(
                                            transactionId: Int,
                                            unitId: Int, fc: ModbusFunction,
                                            val exception: Option[ModbusException]
                                         ) extends ModbusResponse(transactionId, unitId, fc)
{
   override def toString = s"${this.getClass.getSimpleName}($transactionId, $unitId, $fc, ${exception})"
}

class ReadCoilsErrorResponse(
                               transactionId: Int,
                               unitId: Int,
                               exception: Option[ModbusException]
                            ) extends ModbusErrorResponse(transactionId, unitId, ReadCoils, exception)

class ReadDiscreteInputErrorResponse(
                                       transactionId: Int,
                                       unitId: Int,
                                       exception: Option[ModbusException]
                                    ) extends ModbusErrorResponse(transactionId, unitId, ReadDiscreteInput, exception)

class ReadHoldingRegisterErrorResponse(
                                         transactionId: Int,
                                         unitId: Int,
                                         exception: Option[ModbusException]
                                      ) extends ModbusErrorResponse(transactionId, unitId, ReadHoldingRegister, exception)

class ReadInputRegisterErrorResponse(
                                       transactionId: Int,
                                       unitId: Int,
                                       exception: Option[ModbusException]
                                    ) extends ModbusErrorResponse(transactionId, unitId, ReadInputRegister, exception)