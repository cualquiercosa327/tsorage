package be.cetic.tsorage.collector.unsigned

import java.nio.ByteBuffer

/**
 * Unsigned short.
 */
class UShort(underlying: Array[Byte]) extends UnsignedType(underlying)
{
   def toInt: Int = {
      val bi = BigInt(underlying)

      if(bi < 0) bi.toInt + 256
      else bi.toInt
   }
}

object UShort
{
   def fromInt(value: Int): UShort =
   {
      val buffer = ByteBuffer.allocate(4).putInt(value).array().takeRight(2)
      new UShort(buffer)
   }

   def fromBigEndianByteArray(bytes: Array[Byte]): UShort =
      new UShort(bytes)

   def fromLittleEndianByteArray(bytes: Array[Byte]): UShort =
      new UShort(bytes.reverse)
}
