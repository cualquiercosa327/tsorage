package be.cetic.tsorage.collector.modbus

import be.cetic.tsorage.common.messaging.Message

sealed abstract class ModbusErrorResponse(transactionId: Int, unitId: Int, fc: Int, val exception: Option[ModbusException]) extends ModbusResponse(transactionId, unitId, fc)
{
   override def toString = s"${this.getClass.getSimpleName}($transactionId, $unitId, $fc, ${exception})"
}

class ReadCoilsErrorResponse(
                               transactionId: Int,
                               unitId: Int,
                               exception: Option[ModbusException]
                            ) extends ModbusErrorResponse(transactionId, unitId, 0x01, exception)

class ReadDiscreteInputErrorResponse(
                                       transactionId: Int,
                                       unitId: Int,
                                       exception: Option[ModbusException]
                                    ) extends ModbusErrorResponse(transactionId, unitId, 0x02, exception)

class ReadHoldingRegisterErrorResponse(
                                         transactionId: Int,
                                         unitId: Int,
                                         exception: Option[ModbusException]
                                      ) extends ModbusErrorResponse(transactionId, unitId, 0x03, exception)

class ReadInputRegisterErrorResponse(
                                       transactionId: Int,
                                       unitId: Int,
                                       exception: Option[ModbusException]
                                    ) extends ModbusErrorResponse(transactionId, unitId, 0x04, exception)