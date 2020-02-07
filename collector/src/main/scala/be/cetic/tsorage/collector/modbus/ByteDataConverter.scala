package be.cetic.tsorage.collector.modbus

/**
 * Converter for the Byte data type.
 */
object ByteDataConverter extends DataConverter
{
   /**
    * Converts a byte array of one byte representing an unsigned byte into an integer
    * @param bytes An array of one byte to be converted to an unsigned byte.
    * @return The value of the unsigned byte
    */
   def asUnsignedByte(bytes: Array[Byte], littleEndian: Boolean = true) =
   {
      assert(bytes.size == 1)
      DataConverter.bytesToUnsignedInt(bytes, littleEndian)
   }

   /**
    * Converts an unsigned byte into a byte array.
    * @param value   The unsigned byte to convert.
    * @return  The byte array encoding the unsigned byte array.
    */
   def fromUnsignedByte(value: Int): Array[Byte] =
   {
      assert(value >= 0)

      BigInt(value).toByteArray.takeRight(1)
   }

   def fromSignedByte(value: Int, littleEndian: Boolean = true): Array[Byte] =
   {
      assert(value <= Byte.MaxValue)
      assert(value >= Byte.MinValue)

      Array[Byte](value.toByte)
   }

   def asSignedByte(bytes: Array[Byte], littleEndian: Boolean = true): Int =
   {
      assert(bytes.size == 1)
      BigInt(bytes).toInt
   }
}
