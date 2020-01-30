package be.cetic.tsorage.collector.modbus

/**
 * Utility object for converting data from and to byte array.
 */
object DataConverter
{
   /**
    * @param bytes An array of one byte to be converted to an unsigned byte.
    */
   def asUnsignedByte(bytes: Array[Byte]) =
   {
      assert(bytes.size == 1)

      val raw = BigInt(bytes).toInt

      if(raw < 0) ~raw
      else raw
   }

   def fromUnsignedByte(value: Int): Array[Byte] =
   {
      assert(value >= 0)

      BigInt(value).toByteArray.takeRight(1)
   }

   /**
    * @param bytes An array of two bytes to be converted to an unsigned short.
    */
   def asUnsignedShort(bytes: Array[Byte]) =
   {


   }
}
