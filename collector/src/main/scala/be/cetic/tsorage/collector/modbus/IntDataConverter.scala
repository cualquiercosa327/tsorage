package be.cetic.tsorage.collector.modbus

/**
 * Converter for the integer data type.
 */
object IntDataConverter extends DataConverter
{
   def fromUnsignedInt(value: Long, littleEndian: Boolean = true): Array[Byte] =
   {
      assert(value >= 0)

      val bytes = padLeft(BigInt(value).toByteArray, 4)

      if(littleEndian) bytes.reverse
      else bytes
   }

   /**
    * @param bytes An array of two bytes to be converted to an unsigned short.
    */
   def asUnsignedInt(bytes: Array[Byte], littleEndian: Boolean = true): Long =
   {
      assert(bytes.size == 4)
      bytesToUnsignedInt(bytes, littleEndian)
   }

   def fromSignedInt(value: Int, littleEndian: Boolean = true): Array[Byte] =
   {
      assert(value <= Int.MaxValue)
      assert(value >= Int.MinValue)

      val posBE = BigInt(Math.abs(value)).toByteArray

      val bytes = if(value >= 0) posBE.reverse.padTo(4, 0x00.toByte)
                  else ((~BigInt(posBE))+1).toByteArray.reverse.padTo(4, 0xff.toByte)

      if(littleEndian) bytes
      else bytes.reverse
   }

   def asSignedInt(bytes: Array[Byte], littleEndian: Boolean = true) =
   {
      assert(bytes.size == 4)

      if(littleEndian) BigInt(bytes.reverse).toInt
      else BigInt(bytes).toInt
   }
}
