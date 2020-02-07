package be.cetic.tsorage.collector.modbus

import java.nio.ByteBuffer

import scala.collection.mutable

/**
 * Converter for short data type.
 */
object ShortDataConverter extends DataConverter
{
   def fromUnsignedShort(value: Int, littleEndian: Boolean = true): Array[Byte] =
   {
      assert(value >= 0)

      val bytes = DataConverter.padLeft(BigInt(value).toByteArray.takeRight(2), 2)

      if(littleEndian) bytes.reverse
      else bytes
   }

   /**
    * @param bytes An array of two bytes to be converted to an unsigned short.
    */
   def asUnsignedShort(bytes: Array[Byte], littleEndian: Boolean = true) =
   {
      assert(bytes.size == 2)
      DataConverter.bytesToUnsignedInt(bytes, littleEndian)
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
      assert(bytes.length == 2, s"2 bytes expected, got ${bytes.length} instead: ${bytes.map(b => b & 0xFF).mkString("[", ",", "]")}")

      if(littleEndian) BigInt(bytes.reverse).toInt
      else BigInt(bytes).toInt
   }
}
