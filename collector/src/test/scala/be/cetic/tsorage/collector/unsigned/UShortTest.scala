package be.cetic.tsorage.collector.unsigned

import org.scalatest.{FlatSpec, Matchers}

class UShortTest extends FlatSpec with Matchers
{
   "The big endian representation of a UShort" should "be two-byte array for small values" in {
      val array = UShort.fromInt(42).toBigEndianByteArray
      array.length shouldBe 1
   }
}
