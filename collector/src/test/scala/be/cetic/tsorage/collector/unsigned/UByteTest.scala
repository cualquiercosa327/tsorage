package be.cetic.tsorage.collector.unsigned

import org.scalatest.{FlatSpec, Matchers}


class UByteTest extends FlatSpec with Matchers
{
   "The big endian representation of a UByte" should "be one-byte array for small values" in {
      val array = UByte.fromInt(42).toBigEndianByteArray
      array.length shouldBe 1
   }

   it should "be one-byte array for large values" in {
      val array = UByte.fromInt(200).toBigEndianByteArray
      array.length shouldBe 1
   }

   it should "correspond to the correct internal representation of its value for small values" in {
      val array = UByte.fromInt(42).toBigEndianByteArray
      array(0) shouldBe 42
   }

   it should "correspond to the correct internal representation of its value for large values" in {
      val array = UByte.fromInt(200).toBigEndianByteArray
      array(0) shouldBe (200 - 256)
   }

   "A UByte" should "be correctly converted back to int for small values" in {
      UByte.fromInt(42).toInt shouldBe 42
   }

   it should "be correctly converted back to int for large values" in {
      UByte.fromInt(200).toInt shouldBe 200
   }
}
