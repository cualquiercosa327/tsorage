package be.cetic.tsorage.collector.modbus
import be.cetic.tsorage.common.messaging.Message


sealed abstract class ModbusValidResponse(
                                     transactionId: Int,
                                     unitId: Int,
                                     function: ModbusFunction,
                                     val data: Array[Byte]
                                  ) extends ModbusResponse(transactionId, unitId, function)
{
  override def toString = s"${this.getClass.getSimpleName}($transactionId, $unitId, ${data.map(byte => (byte & 0xFF).toHexString).mkString("[", ",", "]")})"
}

class ReadCoilsValidResponse(
                               transactionId: Int,
                               unitId: Int,
                               data: Array[Byte]
                            ) extends ModbusValidResponse(transactionId, unitId, ReadCoils, data)

class ReadDiscreteInputValidResponse(
                                       transactionId: Int,
                                       unitId: Int,
                                       data: Array[Byte]
                                    ) extends ModbusValidResponse(transactionId, unitId, ReadDiscreteInput, data)

class ReadHoldingRegisterValidResponse(
                                         transactionId: Int,
                                         unitId: Int,
                                         data: Array[Byte]
                                      ) extends ModbusValidResponse(transactionId, unitId, ReadHoldingRegister, data)


class ReadInputRegisterValidResponse(
                                       transactionId: Int,
                                       unitId: Int,
                                       data: Array[Byte]
                                    ) extends ModbusValidResponse(transactionId, unitId, ReadInputRegister, data)