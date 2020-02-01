package be.cetic.tsorage.collector.modbus

import java.nio.ByteBuffer

import scala.collection.JavaConverters._

/**
 * Utility object for converting data from and to byte array.
 */
trait DataConverter
{
   protected def bytesToUnsignedInt(bytes: Array[Byte], littleEndian: Boolean): Int = {

      val padded = if(littleEndian) bytes.padTo(4, (0x0).toByte)
                   else bytes.reverse.padTo(4, 0x0.toByte).reverse

      val buffer = java.nio.ByteBuffer.wrap(padded, 0, 4)
      if (littleEndian) buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt
      else buffer.getInt
   }

   protected def padLeft(bytes: Array[Byte], length: Int): Array[Byte] =
   {
      bytes.reverse.padTo(length, 0x0.toByte).reverse
   }
}
