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
   private val PROTOCOL_ID: Array[Byte] = ShortDataConverter.fromUnsignedShort(0, false)

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

   /**
    * Creates a 7-byte frame prefix for TCP requests.
    * @return
    */
   private def createMBAP(transactionID: Int, length: Int, unitID: Int): Array[Byte] =
   {
      val buffer = ByteBuffer
         .allocate(7)
         .put(ShortDataConverter.fromUnsignedShort(transactionID, false))
         .put(PROTOCOL_ID)
         .put(ShortDataConverter.fromUnsignedShort(length+1, false))
         .put(ByteDataConverter.fromUnsignedByte(unitID))
         .array


      assert(buffer.size == 7)

      buffer
   }

   /**
    * Creates a TCP version of the request.
    *
    * http://www.simplymodbus.ca/TCP.htm
    *
    * @return
    */
   def createTCPFrame(transactionID: Int, unitID: Int): Array[Byte] =
   {
      assert(transactionID >= 0)
      assert(transactionID <= 2 * Short.MaxValue)

      assert(unitID >= 0)
      assert(unitID <= 2 * Short.MaxValue)

      val payload = createPayloadBuffer().array
      val mbap = createMBAP(transactionID, payload.size, unitID)

      val buffer = ByteBuffer.allocate(7 + payload.size)
         .put(mbap)
         .put(payload)
         .array

      buffer
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

