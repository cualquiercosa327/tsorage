package be.cetic.tsorage.collector.modbus

import java.nio.ByteBuffer

/**
 * A request for a Modbus slave.
 *
 * http://www.ozeki.hu/index.php?owpn=5854
 * https://www.modbustools.com/modbus.html#function02
 */
sealed class ModbusRequest(
                             registerNumber: Int,
                             registerCount: Int,
                             fc: Int
                          )
{
   assert(registerNumber >= 0)
   assert(registerCount >= 0)

   private def createPayloadBuffer(): ByteBuffer =
   {
      ByteBuffer.allocate(5)
         .put(ByteDataConverter.fromUnsignedByte(fc))
         .put(ShortDataConverter.fromUnsignedShort(registerNumber, false))
         .put(ShortDataConverter.fromUnsignedShort(registerCount, false))
   }

   def createRTUFrame(unitIdentifier: Int): Array[Byte] = {
      assert(unitIdentifier >= 0)

      val payload = createPayloadBuffer()

      val prefixBuffer = ByteBuffer.allocate(6)
         .put(ByteDataConverter.fromUnsignedByte(unitIdentifier))
         .put(payload.array)
         .array

      val crc = CRC16.calculateCRC(prefixBuffer)

      val buffer = ByteBuffer.allocate(8)
         .put(prefixBuffer)
         .put(crc.array)

      buffer.array()
   }
}

/**
 * Modbus function 1
 */
class ReadCoils(registerNumber: Int, registerCount: Int) extends ModbusRequest(
   registerNumber,
   registerCount,
   0x1
)

/**
 * Modbus function 2
 * @param registerNumber
 * @param registerCount
 */
class ReadDiscreteInput(registerNumber: Int, registerCount: Int) extends ModbusRequest(
   registerNumber,
   registerCount,
   0x2
)

/**
 * Modbus function 3
 * @param registerNumber
 * @param registerCount
 */
class ReadHoldingRegister(registerNumber: Int, registerCount: Int) extends ModbusRequest(
   registerNumber,
   registerCount,
   0x3
)

/**
 * Modbus function 4
 * @param registerNumber
 * @param registerCount
 */
class ReadInputRegister(registerNumber: Short, registerCount: Short) extends ModbusRequest(
   registerNumber,
   registerCount,
   0x4
)

