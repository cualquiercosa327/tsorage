package be.cetic.tsorage.collector.modbus

import java.nio.ByteBuffer

/**
 * A request for a Modbus slave.
 */
sealed class ModbusRequest(
                             unitIdentifier: Byte,
                             registerNumber: Short,
                             registerCount: Short,
                             fc: Byte
                          )
{
   def createFrame: Array[Byte] = {
      val buffer = ByteBuffer
         .allocateDirect(5)
         .putShort(0.toShort)    // transaction identifier
         .putShort(0.toShort)    // protocol identifier
         .putShort(6.toShort)    // Length field
         .put(unitIdentifier)    // Unit identifier, not used for TCP requests
         .put(fc)                // Modbus function code
         .putShort(registerNumber) // address
         .putShort(registerCount)   // Number of registers

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

