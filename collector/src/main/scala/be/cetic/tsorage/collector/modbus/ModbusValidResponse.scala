package be.cetic.tsorage.collector.modbus
import be.cetic.tsorage.common.messaging.Message


sealed abstract class ModbusValidResponse(
                                     transactionId: Int,
                                     unitId: Int,
                                     fc: Int,
                                     val data: Array[Byte]
                                  ) extends ModbusResponse(transactionId, unitId, fc)
{
  override def toString = s"${this.getClass.getSimpleName}($transactionId, $unitId, ${data.map(byte => (byte & 0xFF).toHexString).mkString("[", ",", "]")})"
}

class ReadCoilsValidResponse(
                               transactionId: Int,
                               unitId: Int,
                               data: Array[Byte]
                            ) extends ModbusValidResponse(transactionId, unitId, 0x01, data)

class ReadDiscreteInputValidResponse(
                                       transactionId: Int,
                                       unitId: Int,
                                       data: Array[Byte]
                                    ) extends ModbusValidResponse(transactionId, unitId, 0x02, data)

class ReadHoldingRegisterValidResponse(
                                         transactionId: Int,
                                         unitId: Int,
                                         data: Array[Byte]
                                      ) extends ModbusValidResponse(transactionId, unitId, 0x03, data)


class ReadInputRegisterValidResponse(
                                       transactionId: Int,
                                       unitId: Int,
                                       data: Array[Byte]
                                    ) extends ModbusValidResponse(transactionId, unitId, 0x04, data)