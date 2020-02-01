package be.cetic.tsorage.collector.modbus

import java.nio.ByteBuffer

import scala.collection.JavaConverters._

/**
 * Utility object for converting data from and to byte array.
 */
object DataConverter
{
   private def bytesToUnsignedInt(bytes: Array[Byte], littleEndian: Boolean): Int = {

      val padded = if(littleEndian) bytes.padTo(4, (0x0).toByte)
                   else bytes.reverse.padTo(4, 0x0.toByte).reverse

      val buffer = java.nio.ByteBuffer.wrap(padded, 0, 4)
      if (littleEndian) buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt
      else buffer.getInt
   }

   private def padLeft(bytes: Array[Byte], length: Int): Array[Byte] =
   {
      bytes.reverse.padTo(length, 0x0.toByte).reverse
   }

   /**
    * Converts a byte array of one byte representing an unsigned byte into an integer
    * @param bytes An array of one byte to be converted to an unsigned byte.
    * @return The value of the unsigned byte
    */
   def asUnsignedByte(bytes: Array[Byte], littleEndian: Boolean = true) =
   {
      assert(bytes.size == 1)
      bytesToUnsignedInt(bytes, littleEndian)
   }

   /**
    * Converts an unsigned byte into a byte array.
    * @param value   The unsigned byte to convert.
    * @return  The byte array encoding the unsigned byte array.
    */
   def fromUnsignedByte(value: Int, littleEndian: Boolean = true): Array[Byte] =
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

   def fromUnsignedShort(value: Int, littleEndian: Boolean = true): Array[Byte] =
   {
      assert(value >= 0)

      val bytes = padLeft(BigInt(value).toByteArray.takeRight(2), 2)

      if(littleEndian) bytes.reverse
      else bytes
   }

   /**
    * @param bytes An array of two bytes to be converted to an unsigned short.
    */
   def asUnsignedShort(bytes: Array[Byte], littleEndian: Boolean = true) =
   {
      assert(bytes.size == 2)
      bytesToUnsignedInt(bytes, littleEndian)
   }

   def fromSignedShort(value: Int, littleEndian: Boolean = true): Array[Byte] =
   {
      assert(value <= Short.MaxValue)
      assert(value >= Short.MinValue)

      val posBE = BigInt(Math.abs(value)).toByteArray

      val bytes = if(value >= 0) posBE.reverse.padTo(2, 0x00.toByte)
                  else ((~BigInt(posBE))+1).toByteArray.reverse.padTo(2, 0xff.toByte)

      if(littleEndian) bytes
      else bytes.reverse
   }

   def asSignedShort(bytes: Array[Byte], littleEndian: Boolean = true) =
   {
      assert(bytes.size == 2)

      if(littleEndian) BigInt(bytes.reverse).toInt
      else BigInt(bytes).toInt
   }

   // ===

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
