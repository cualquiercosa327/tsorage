package be.cetic.tsorage.collector.modbus

import java.nio.{ByteBuffer, ByteOrder, ShortBuffer}

import scala.collection.JavaConverters._

/**
 * Utility object for converting data from and to byte array.
 *
 * https://store.chipkin.com/articles/how-real-floating-point-and-32-bit-data-is-encoded-in-modbus-rtu-messages
 * https://www.binaryconvert.com
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

object DataConverter
{
   /**
    * Converts a byte array in order to uniformize it.
    *
    * https://store.chipkin.com/articles/how-real-floating-point-and-32-bit-data-is-encoded-in-modbus-rtu-messages
    *
    * @param bytes   A short arra.
    * @return  The byte array, with the bytes placed in a big endian order.
    */
   def orderNormalization(bytes: Array[Byte], byteSwap: Boolean, wordSwap: Boolean): Array[Byte] =
   {
      val wordFlipped = if(!wordSwap) Array(0, 1, 2, 3)
                        else Array(2, 3, 0, 1)


      val byteFlipped = if(!byteSwap) wordFlipped
                        else Array(
                           wordFlipped(1),
                           wordFlipped(0),
                           wordFlipped(3),
                           wordFlipped(2)
                        )

      byteFlipped.map(index => bytes(index))
   }
}
