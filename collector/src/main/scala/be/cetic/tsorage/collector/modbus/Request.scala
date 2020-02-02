package be.cetic.tsorage.collector.modbus

import java.nio.ByteBuffer

/**
 * A request for a Modbus slave.
 *
 * http://www.ozeki.hu/index.php?owpn=5854
 * https://www.modbustools.com/modbus.html#function02
 */
sealed class ModbusRequest(
                             unitIdentifier: Int,
                             registerNumber: Int,
                             registerCount: Int,
                             fc: Int
                          )
{
   assert(unitIdentifier >= 0)
   assert(registerNumber >= 0)
   assert(registerCount >= 0)

   def createFrame: Array[Byte] = {
      val payload = ByteBuffer
         .allocate(6)
         .put(ByteDataConverter.fromUnsignedByte(unitIdentifier))
         .put(ByteDataConverter.fromUnsignedByte(fc))
         .put(ShortDataConverter.fromUnsignedShort(registerNumber, false))
         .put(ShortDataConverter.fromUnsignedShort(registerCount, false))

      val crc = CRC16.calculateCRC(payload.array())

      val buffer = ByteBuffer.allocate(8)
         .put(payload.array)
         .put(crc.array)

      buffer.array()
   }
}

/**
 * Modbus function 1
 */
class ReadCoils(unitIdentifier: Int, registerNumber: Int, registerCount: Int) extends ModbusRequest(
   unitIdentifier,
   registerNumber,
   registerCount,
   0x1
)

/**
 * Modbus function 2
 * @param registerNumber
 * @param registerCount
 */
class ReadDiscreteInput(unitIdentifier: Int, registerNumber: Int, registerCount: Int) extends ModbusRequest(
   unitIdentifier,
   registerNumber,
   registerCount,
   0x2
)

/**
 * Modbus function 3
 * @param registerNumber
 * @param registerCount
 */
class ReadHoldingRegister(unitIdentifier: Int, registerNumber: Int, registerCount: Int) extends ModbusRequest(
   unitIdentifier,
   registerNumber,
   registerCount,
   0x3
)

/**
 * Modbus function 4
 * @param registerNumber
 * @param registerCount
 */
class ReadInputRegister(unitIdentifier: Byte, registerNumber: Short, registerCount: Short) extends ModbusRequest(
   unitIdentifier,
   registerNumber,
   registerCount,
   0x4
)

