package be.cetic.tsorage.collector.modbus

import java.nio.ByteBuffer

/**
 * A request for a Modbus slave.
 *
 * http://www.ozeki.hu/index.php?owpn=5854
 */
sealed class ModbusRequest(
                             unitIdentifier: Int,
                             registerNumber: Int,
                             registerCount: Short,
                             fc: Int
                          )
{

   assert(unitIdentifier >= 0)
   assert(registerNumber >= 0)
   assert(registerCount >= 0)

   /**
    * https://sitelec.org/cours/abati/modbus.htm
    */
   private def calculateCRC() = {
      ByteBuffer.wrap(Array[Byte](0xff.toByte, 0xff.toByte))
   }

   def createFrame: Array[Byte] = {
      val buffer = ByteBuffer
         .allocateDirect(5)
         .put(ByteDataConverter.fromUnsignedByte(unitIdentifier))
         .put(ByteDataConverter.fromUnsignedByte(fc))
         .put(ShortDataConverter.fromUnsignedShort(registerNumber, false))
         .put(ShortDataConverter.fromUnsignedShort(registerCount, false))
         //.put(ShortDataConverter.fromUnsignedShort(crc, false))

      buffer.array()
   }
}

/**
 * Modbus function 1
 */
class ReadCoils(unitIdentifier: Byte, registerNumber: Short, registerCount: Short) extends ModbusRequest(
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
class ReadDiscreteInput(unitIdentifier: Byte, registerNumber: Short, registerCount: Short) extends ModbusRequest(
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
class ReadHoldingRegister(unitIdentifier: Byte, registerNumber: Short, registerCount: Short) extends ModbusRequest(
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

