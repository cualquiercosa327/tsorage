package be.cetic.tsorage.collector.modbus.data

import java.nio.ByteBuffer

/**
 * https://store.chipkin.com/articles/how-real-floating-point-and-32-bit-data-is-encoded-in-modbus-rtu-messages
 */
object FloatDataConverter extends DataConverter
{
   /**
    *
    * @param bytes   An array of 4 bytes to be converted to an unsigned float.
    *                The bytes are a big endian representation of the integer.
    * @return  The unsigned float corresponding to the byte array.
    */
   def asSignedFloat(bytes: Array[Byte]): Float =
   {
      assert(bytes.length == 4)
      ByteBuffer.wrap(bytes).getFloat()
   }
}
