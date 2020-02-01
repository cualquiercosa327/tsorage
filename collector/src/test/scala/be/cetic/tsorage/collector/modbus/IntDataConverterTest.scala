package be.cetic.tsorage.collector.modbus

import org.scalatest.{FlatSpec, Matchers}

/**
 * https://www.binaryconvert.com
 */
class IntDataConverterTest extends FlatSpec with Matchers
{
   "A small unsigned int" should "be represented by the right byte array" in {
      IntDataConverter.fromUnsignedInt(42, true) shouldBe Array[Byte](0x2a, 0, 0, 0)
      IntDataConverter.fromUnsignedInt(42, false) shouldBe Array[Byte](0, 0, 0, 0x2a)
   }

   "A large unsigned int" should "be represented by the right byte array" in {
      IntDataConverter.fromUnsignedInt(20000000L, true) shouldBe
         Array[Byte](0x00, 0x2d, 0x31, 0x01)
      IntDataConverter.fromUnsignedInt(20000000L, false) shouldBe
         Array[Byte](0x01, 0x31, 0x2d, 0x00)
   }

   "A byte array representing a small unsigned int" should "be converted into the right value" in {
      IntDataConverter.asUnsignedInt(Array[Byte](0xd0.toByte, 0x07, 0x00, 0x00), true) shouldBe 2000
      IntDataConverter.asUnsignedInt(Array[Byte](0x00, 0x00, 0x07, 0xd0.toByte), false) shouldBe 2000}

   "A byte array representing a large unsigned int" should "be converted into the right value" in {
      IntDataConverter.asUnsignedInt(Array[Byte](0x00, 0x2d, 0x31, 0x01), true) shouldBe 20000000
      IntDataConverter.asUnsignedInt(Array[Byte](0x01, 0x31, 0x2d, 0x00), false) shouldBe 20000000
   }

   "A small positive signed int" should "be represented by the right byte array" in {
      IntDataConverter.fromSignedInt(42, true) shouldBe
         Array[Byte](0x2a, 0x00, 0x00, 0x00)
      IntDataConverter.fromSignedInt(42, false) shouldBe
         Array[Byte](0x00, 0x00, 0x00, 0x2a)
   }

   "A byte array representing a small positive signed int" should "be converted into the right value" in {
      IntDataConverter.asSignedInt(Array[Byte](0x2a, 0x00, 0x00, 0x00), true) shouldBe 42
      IntDataConverter.asSignedInt(Array[Byte](0x00, 0x00, 0x00, 0x2a), false) shouldBe 42
   }

   "A small negative signed int" should "be represented by the right byte array" in {
      IntDataConverter.fromSignedInt(-42, true) shouldBe
         Array[Byte](0xd6.toByte, 0xff.toByte, 0xff.toByte, 0xff.toByte)
      IntDataConverter.fromSignedInt(-42, false) shouldBe
         Array[Byte](0xff.toByte, 0xff.toByte, 0xff.toByte, 0xd6.toByte)
   }

   "A byte array representing a small negative signed int" should "be converted into the right value" in {
      IntDataConverter.asSignedInt(Array(0xd6.toByte, 0xff.toByte, 0xff.toByte, 0xff.toByte), true) shouldBe -42
      IntDataConverter.asSignedInt(Array(0xff.toByte, 0xff.toByte, 0xff.toByte, 0xd6.toByte), false) shouldBe -42
   }

   "A large positive signed int" should "be represented by the right byte array" in {
      IntDataConverter.fromSignedInt(20000000, true) shouldBe
         Array[Byte](0x00, 0x2d, 0x31, 0x01)
      IntDataConverter.fromSignedInt(20000000, false) shouldBe
         Array[Byte](0x01, 0x31, 0x2d, 0x00)
   }

   "A byte array representing a large positive signed int" should "be converted into the right value" in {
      IntDataConverter.asSignedInt(Array(0x00, 0x2d, 0x31, 0x01), true) shouldBe 20000000
      IntDataConverter.asSignedInt(Array(0x01, 0x31, 0x2d, 0x00), false) shouldBe 20000000
   }

   "A large negative signed int" should "be represented by the right byte array" in {
      IntDataConverter.fromSignedInt(-20000000, true) shouldBe
         Array[Byte](0x00, 0xd3.toByte, 0xce.toByte, 0xfe.toByte)
      IntDataConverter.fromSignedInt(-20000000, false) shouldBe
         Array[Byte](0xfe.toByte, 0xce.toByte, 0xd3.toByte, 0x00)
   }

   "A byte array representing a large negative signed int" should "be converted into the right value" in {
      IntDataConverter.asSignedInt(Array(0x00, 0xd3.toByte, 0xce.toByte, 0xfe.toByte), true) shouldBe -20000000
      IntDataConverter.asSignedInt(Array(0xfe.toByte, 0xce.toByte, 0xd3.toByte, 0x00), false) shouldBe -20000000
   }
}
