package be.cetic.tsorage.collector.modbus

/**
 * Converter for the integer data type.
 */
object IntDataConverter extends DataConverter
{
   /**
    * @param bytes An array of 4 bytes to be converted to an unsigned int.
    *              The bytes are a big endian representation of the integer.
    *
    */
   def asUnsignedInt(bytes: Array[Byte]): Long =
   {
      assert(bytes.size == 4)
      bytesToUnsignedInt(bytes, false)
   }

   /**
    * @param bytes   An array of 4 bytes to be converted to a signed int.
    *                The bytes are a big endian representation of the integer.
    * @return
    */

   def asSignedInt(bytes: Array[Byte]) =
   {
      assert(bytes.size == 4)
      BigInt(bytes).toInt
   }
}
