package be.cetic.tsorage.collector.unsigned

import java.nio.ByteBuffer

/**
 * Unsigned byte
 */
class UByte(underlying: Array[Byte]) extends UnsignedType(underlying)
{
   def toInt: Int = {
      val bi = BigInt(underlying)

      if(bi < 0) bi.toInt + 256
      else bi.toInt
   }
}


object UByte
{
   def fromInt(value: Int): UByte =
   {
       val buffer = Array(ByteBuffer.allocate(4).putInt(value).array().last)
       new UByte(buffer)
   }

   def fromBigEndianByteArray(bytes: Array[Byte]): UByte =
      new UByte(bytes)

   def fromLittleEndianByteArray(bytes: Array[Byte]): UByte =
      new UByte(bytes.reverse)
}