package be.cetic.tsorage.collector.unsigned

/**
 * Unsigned type
 */
class UnsignedType(val underlying: Array[Byte])
{
   def toBigEndianByteArray: Array[Byte] = underlying
   def toLittleEndianByteArray: Array[Byte] = underlying.reverse
}
