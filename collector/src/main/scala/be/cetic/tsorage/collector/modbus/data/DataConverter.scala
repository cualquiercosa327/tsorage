package be.cetic.tsorage.collector.modbus.data

import scala.collection.mutable.ListBuffer

/**
 * Utility object for converting data from and to byte array.
 *
 * https://store.chipkin.com/articles/how-real-floating-point-and-32-bit-data-is-encoded-in-modbus-rtu-messages
 * https://www.binaryconvert.com
 */
trait DataConverter
{

}

object DataConverter
{
   def bytesToUnsignedInt(bytes: Array[Byte], littleEndian: Boolean): Long = {

      val padded = if(littleEndian) bytes.padTo(8, (0x0).toByte)
                   else bytes.reverse.padTo(8, 0x0.toByte).reverse

      val buffer = java.nio.ByteBuffer.wrap(padded, 0, 8)
      if (littleEndian) buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN).getLong
      else buffer.getLong
   }

   def padLeft(bytes: Array[Byte], length: Int): Array[Byte] =
   {
      bytes.reverse.padTo(length, 0x0.toByte).reverse
   }

   /**
    * Converts a byte array in order to uniformize it.
    *
    * https://store.chipkin.com/articles/how-real-floating-point-and-32-bit-data-is-encoded-in-modbus-rtu-messages
    *
    * @param bytes   A byte array. The number of bytes must be a multiple of 4.
    * @return  The byte array, with the bytes placed in a big endian order.
    */
   def orderNormalization(bytes: Array[Byte], byteSwap: Boolean, wordSwap: Boolean): Array[Byte] =
   {
      assert(bytes.length % 4 == 0)
      val nbBytes = bytes.length
      val nbWords = nbBytes / 2

      (byteSwap, wordSwap) match {
         case (false, false) => bytes
         case (true, true) => bytes.reverse
         case (true, false) => {
            val tmp = new ListBuffer[Byte]()

            (0 until nbWords).foreach(wordId => {
               tmp.append(bytes(2*wordId+1), bytes(2*wordId))
            })

            tmp.toArray
         }
         case (false, true) => {
            val tmp = new ListBuffer[Byte]()

            (0 until nbWords).reverse.foreach(wordId => {
               tmp.append(bytes(2*wordId), bytes(2*wordId+1))
            })

            tmp.toArray
         }
      }
   }
}
